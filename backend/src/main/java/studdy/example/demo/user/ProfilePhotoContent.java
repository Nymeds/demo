package studdy.example.demo.user;

import java.util.Arrays;

public record ProfilePhotoContent(byte[] content, String contentType) {

    public ProfilePhotoContent {
        content = Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
