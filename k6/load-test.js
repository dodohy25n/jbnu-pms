/**
 * k6 성능 테스트 스크립트 - jbnu-pms
 *
 * 사전 준비:
 *   1. k6 설치: https://k6.io/docs/getting-started/installation/
 *   2. 아래 CONFIG에 실제 값 입력 (테스트 계정, spaceId, projectId, userId)
 *   3. 실행: k6 run k6/load-test.js
 *   4. JSON 결과 저장: k6 run --out json=k6/result.json k6/load-test.js
 *
 * K8s 모니터링 (별도 터미널):
 *   kubectl get pods -n jbnu-pms -w
 *   kubectl top pods -n jbnu-pms        (metrics-server 필요)
 *   kubectl describe deployment jbnu-pms-app -n jbnu-pms
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// ─────────────────────────────────────────────
// 1. 설정: 실제 값으로 교체 필요
// ─────────────────────────────────────────────
const CONFIG = {
  BASE_URL: 'http://your-server-ip:port',  // 실제 서버 주소로 교체
  TEST_USER: {
    email: 'your-email@example.com',       // 테스트 계정 이메일로 교체
    password: 'your-password',             // 테스트 계정 비밀번호로 교체
  },
  // 미리 만들어둔 스페이스/프로젝트/유저 ID로 교체
  SPACE_ID: 1,
  PROJECT_ID: 1,
  MEMBER_USER_ID: 1,
};

// ─────────────────────────────────────────────
// 2. 부하 시나리오 정의
// ─────────────────────────────────────────────
export const options = {
  scenarios: {
    // 시나리오 A: 대시보드 읽기 (READ 부하, 주요 시나리오)
    dashboard_read: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },  // 30초 동안 10명으로 증가
        { duration: '1m',  target: 30 },  // 1분 동안 30명 유지
        { duration: '30s', target: 50 },  // 30초 동안 50명으로 증가 (스파이크)
        { duration: '1m',  target: 50 },  // 1분 유지
        { duration: '30s', target: 0 },   // 30초 동안 종료
      ],
      exec: 'dashboardScenario',
    },
    // 시나리오 B: 태스크 생성/조회 (WRITE 부하)
    task_write: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m',  target: 5  },
        { duration: '2m',  target: 10 },
        { duration: '30s', target: 0  },
      ],
      exec: 'taskWriteScenario',
      startTime: '30s',  // 30초 뒤에 시작 (초기 로그인 부하 분산)
    },
  },
  thresholds: {
    // 전체 요청의 95%가 2초 이내 응답
    http_req_duration: ['p(95)<2000'],
    // 에러율 5% 미만
    http_req_failed: ['rate<0.05'],
  },
};

// ─────────────────────────────────────────────
// 3. 커스텀 메트릭
// ─────────────────────────────────────────────
const loginDuration   = new Trend('login_duration');
const dashboardErrors = new Rate('dashboard_errors');
const taskCreateCount = new Counter('task_create_count');

// ─────────────────────────────────────────────
// 4. 헬퍼: 로그인 → accessToken 반환
// ─────────────────────────────────────────────
function login() {
  const res = http.post(
    `${CONFIG.BASE_URL}/auth/login`,
    JSON.stringify({
      email: CONFIG.TEST_USER.email,
      password: CONFIG.TEST_USER.password,
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  loginDuration.add(res.timings.duration);

  const ok = check(res, {
    'login: status 200': (r) => r.status === 200,
    'login: accessToken exists': (r) => {
      try { return JSON.parse(r.body).data.accessToken !== undefined; }
      catch { return false; }
    },
  });

  if (!ok) return null;
  return JSON.parse(res.body).data.accessToken;
}

function authHeaders(token) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };
}

// ─────────────────────────────────────────────
// 5. 시나리오 A: 대시보드 (READ 집중)
//    실제 사용자의 앱 진입 → 대시보드 조회 흐름
// ─────────────────────────────────────────────
export function dashboardScenario() {
  const token = login();
  if (!token) {
    dashboardErrors.add(1);
    sleep(1);
    return;
  }

  const opts = authHeaders(token);

  group('대시보드 로드', () => {
    // 스페이스 목록
    const spacesRes = http.get(`${CONFIG.BASE_URL}/spaces`, opts);
    check(spacesRes, { 'spaces: 200': (r) => r.status === 200 });

    // 프로젝트 목록
    const projectsRes = http.get(
      `${CONFIG.BASE_URL}/projects?spaceId=${CONFIG.SPACE_ID}`, opts
    );
    check(projectsRes, { 'projects: 200': (r) => r.status === 200 });

    // 최근 프로젝트 (대시보드 위젯)
    const recentRes = http.get(
      `${CONFIG.BASE_URL}/projects/recent?spaceId=${CONFIG.SPACE_ID}`, opts
    );
    check(recentRes, { 'recent projects: 200': (r) => r.status === 200 });
  });

  sleep(0.5);

  group('태스크 현황', () => {
    // 긴급 태스크 (대시보드 위젯)
    const urgentRes = http.get(
      `${CONFIG.BASE_URL}/tasks/urgent?spaceId=${CONFIG.SPACE_ID}`, opts
    );
    check(urgentRes, { 'urgent tasks: 200': (r) => r.status === 200 });

    // 내 태스크 요약
    const summaryRes = http.get(
      `${CONFIG.BASE_URL}/tasks/my/summary?spaceId=${CONFIG.SPACE_ID}`, opts
    );
    check(summaryRes, { 'task summary: 200': (r) => r.status === 200 });

    // 프로젝트 태스크 목록
    const tasksRes = http.get(
      `${CONFIG.BASE_URL}/tasks?projectId=${CONFIG.PROJECT_ID}`, opts
    );
    check(tasksRes, { 'tasks list: 200': (r) => r.status === 200 });
  });

  sleep(0.5);

  group('알림 조회', () => {
    const notifRes = http.get(`${CONFIG.BASE_URL}/notifications`, opts);
    check(notifRes, { 'notifications: 200': (r) => r.status === 200 });

    const unreadRes = http.get(`${CONFIG.BASE_URL}/notifications/unread-count`, opts);
    check(unreadRes, { 'unread count: 200': (r) => r.status === 200 });
  });

  sleep(1);
}

// ─────────────────────────────────────────────
// 6. 시나리오 B: 태스크 CRUD (WRITE 부하)
// ─────────────────────────────────────────────
export function taskWriteScenario() {
  const token = login();
  if (!token) { sleep(1); return; }

  const opts = authHeaders(token);

  group('태스크 생성', () => {
    const createRes = http.post(
      `${CONFIG.BASE_URL}/tasks`,
      JSON.stringify({
        projectId: CONFIG.PROJECT_ID,
        title: `부하테스트 태스크 ${Date.now()}`,
        description: 'k6 성능 테스트 중 생성된 태스크',
        priority: 'MEDIUM',
        assigneeIds: [CONFIG.MEMBER_USER_ID],
      }),
      opts
    );

    const created = check(createRes, {
      'task create: 200': (r) => r.status === 200,
    });

    if (created) {
      taskCreateCount.add(1);

      // 생성한 태스크 조회
      try {
        const taskId = JSON.parse(createRes.body).data.id;
        if (taskId) {
          const getRes = http.get(`${CONFIG.BASE_URL}/tasks/${taskId}`, opts);
          check(getRes, { 'task get: 200': (r) => r.status === 200 });
        }
      } catch (_) {}
    }
  });

  sleep(2);
}

// ─────────────────────────────────────────────
// 7. 테스트 종료 후 요약 출력 + JSON 저장
// ─────────────────────────────────────────────
export function handleSummary(data) {
  const m = data.metrics;

  console.log('\n===== 성능 테스트 결과 요약 =====');
  console.log(`총 요청 수     : ${m.http_reqs.values.count}`);
  console.log(`RPS            : ${m.http_reqs.values.rate.toFixed(2)}`);
  console.log(`응답시간 p50   : ${m.http_req_duration.values['med'].toFixed(0)}ms`);
  console.log(`응답시간 p90   : ${m.http_req_duration.values['p(90)'].toFixed(0)}ms`);
  console.log(`응답시간 p95   : ${m.http_req_duration.values['p(95)'].toFixed(0)}ms`);
  console.log(`응답시간 p99   : ${(m.http_req_duration.values['p(99)'] ?? 0).toFixed(0)}ms`);
  console.log(`에러율         : ${(m.http_req_failed.values.rate * 100).toFixed(2)}%`);
  console.log(`생성된 태스크  : ${m.task_create_count?.values.count ?? 0}개`);
  console.log('=================================\n');

  return {
    'k6/result-summary.json': JSON.stringify(data, null, 2),
  };
}
