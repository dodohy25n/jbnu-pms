package jbnu.jbnupms.domain.space.controller;

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
    public ResponseEntity<Long> createSpace(@RequestBody CreateSpaceRequest request) {
        Long spaceId = spaceService.createSpace(request);
        return ResponseEntity.created(URI.create("/spaces/" + spaceId)).body(spaceId);
    }

    @GetMapping("/{spaceId}")
    public ResponseEntity<SpaceDetailResponse> getSpace(@PathVariable Long spaceId) {
        return ResponseEntity.ok(spaceService.getSpace(spaceId));
    }

    @GetMapping
    public ResponseEntity<List<SpaceResponse>> getSpaces(@RequestParam Long userId) {
        return ResponseEntity.ok(spaceService.getSpaces(userId));
    }

    @PutMapping("/{spaceId}")
    public ResponseEntity<Void> updateSpace(@PathVariable Long spaceId, @RequestBody UpdateSpaceRequest request) {
        spaceService.updateSpace(spaceId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{spaceId}")
    public ResponseEntity<Void> deleteSpace(@PathVariable Long spaceId) {
        spaceService.deleteSpace(spaceId);
        return ResponseEntity.ok().build();
    }


}
