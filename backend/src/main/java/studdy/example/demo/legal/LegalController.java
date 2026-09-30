package studdy.example.demo.legal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/legal")
public class LegalController {

    private final LegalVersions legalVersions;

    public LegalController(LegalVersions legalVersions) {
        this.legalVersions = legalVersions;
    }

    @GetMapping("/versions")
    public LegalVersionsResponse versions() {
        return new LegalVersionsResponse(legalVersions.termsVersion(), legalVersions.privacyVersion());
    }
}
