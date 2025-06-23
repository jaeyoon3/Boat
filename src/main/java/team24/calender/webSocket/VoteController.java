package team24.calender.webSocket;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VoteController {

    private final VoteChangeStreamService voteChangeStreamService;

    public VoteController(VoteChangeStreamService voteChangeStreamService) {
        this.voteChangeStreamService = voteChangeStreamService; // 의존성 주입
    }

    @GetMapping("/watch/{gid}/{uid}")
    public String watchVotesByGid(@PathVariable String gid,@PathVariable String uid) {
        // 특정 gid로 문서를 처음에 모두 보내고, 실시간 감지 시작
        voteChangeStreamService.startWatchingForGid(gid,uid);
        return "Started watching votes for gid: " + gid;
    }
}
