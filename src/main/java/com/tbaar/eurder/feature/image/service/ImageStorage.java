package com.tbaar.eurder.feature.image.service;

import com.tbaar.eurder.feature.image.exception.ImageNotFoundException;
import com.tbaar.eurder.feature.image.exception.InvalidImageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ImageStorage {

    public static final String FOLDER = "images";

    private static final Pattern NAME = Pattern.compile("[0-9a-f-]{36}\\.(png|jpg|webp)");
    private static final Map<String, MediaType> MEDIA_TYPES = Map.of(
            "png", MediaType.IMAGE_PNG,
            "jpg", MediaType.IMAGE_JPEG,
            "webp", MediaType.parseMediaType("image/webp"));

    private final Path directory;

    public ImageStorage(@Value("${eurder.images.directory:data/images}") Path directory) {
        this.directory = directory;
    }

    public String store(byte[] content) {
        String extension = extensionOf(content).orElseThrow(InvalidImageException::new);
        String name = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(name), content);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the image " + name, e);
        }
        return FOLDER + "/" + name;
    }

    public StoredImage read(String name) {
        if (!NAME.matcher(name).matches()) {
            throw new ImageNotFoundException(name);
        }
        Path file = directory.resolve(name);
        if (!Files.isRegularFile(file)) {
            throw new ImageNotFoundException(name);
        }
        try {
            String extension = name.substring(name.lastIndexOf('.') + 1);
            return new StoredImage(Files.readAllBytes(file), MEDIA_TYPES.get(extension));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the image " + name, e);
        }
    }

    private static Optional<String> extensionOf(byte[] content) {
        if (startsWith(content, 0, 0x89, 'P', 'N', 'G')) {
            return Optional.of("png");
        }
        if (startsWith(content, 0, 0xff, 0xd8, 0xff)) {
            return Optional.of("jpg");
        }
        if (startsWith(content, 0, 'R', 'I', 'F', 'F') && startsWith(content, 8, 'W', 'E', 'B', 'P')) {
            return Optional.of("webp");
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] content, int offset, int... expected) {
        if (content.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((content[offset + i] & 0xff) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
