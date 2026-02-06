package jbnu.jbnupms.domain.space.controller;

import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.domain.space.dto.CreateSpaceRequest;
import jbnu.jbnupms.domain.space.dto.SpaceDetailResponse;
import jbnu.jbnupms.domain.space.dto.SpaceResponse;
import jbnu.jbnupms.domain.space.dto.UpdateSpaceRequest;
import jbnu.jbnupms.domain.space.service.SpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    @PostMapping
    public ResponseEntity<CommonResponse<Long>> createSpace(@RequestBody CreateSpaceRequest request) {
        Long spaceId = spaceService.createSpace(request);
        return ResponseEntity.created(URI.create("/spaces/" + spaceId))
                .body(CommonResponse.success(spaceId));
    }

    @GetMapping("/{spaceId}")
    public ResponseEntity<CommonResponse<SpaceDetailResponse>> getSpace(@PathVariable Long spaceId) {
        return ResponseEntity.ok(CommonResponse.success(spaceService.getSpace(spaceId)));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<List<SpaceResponse>>> getSpaces(@RequestParam Long userId) {
        return ResponseEntity.ok(CommonResponse.success(spaceService.getSpaces(userId)));
    }

    @PutMapping("/{spaceId}")
    public ResponseEntity<CommonResponse<Void>> updateSpace(@PathVariable Long spaceId, @RequestBody UpdateSpaceRequest request) {
        spaceService.updateSpace(spaceId, request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @DeleteMapping("/{spaceId}")
    public ResponseEntity<CommonResponse<Void>> deleteSpace(@PathVariable Long spaceId) {
        spaceService.deleteSpace(spaceId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}
