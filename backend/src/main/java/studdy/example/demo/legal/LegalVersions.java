package studdy.example.demo.legal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Versões vigentes dos documentos legais, definidas em configuração (app.legal.*).
@Component
public class LegalVersions {

    private final String termsVersion;
    private final String privacyVersion;

    public LegalVersions(
            @Value("${app.legal.terms-version}") String termsVersion,
            @Value("${app.legal.privacy-version}") String privacyVersion
    ) {
        this.termsVersion = termsVersion;
        this.privacyVersion = privacyVersion;
    }

    public String termsVersion() {
        return termsVersion;
    }

    public String privacyVersion() {
        return privacyVersion;
    }
}
