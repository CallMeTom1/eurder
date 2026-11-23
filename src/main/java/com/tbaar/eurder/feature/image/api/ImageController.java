package com.tbaar.eurder.feature.image.api;

import com.tbaar.eurder.feature.image.api.dto.UploadedImageDto;
import com.tbaar.eurder.feature.image.service.ImageStorage;
import com.tbaar.eurder.feature.image.service.StoredImage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;

@RestController
@RequestMapping("/" + ImageStorage.FOLDER)
@Tag(name = "Images", description = "Pictures of the items: uploaded by the admin, read by anybody")
public class ImageController {

    private static final Duration CACHE_DURATION = Duration.ofDays(365);

    private final ImageStorage imageStorage;

    public ImageController(ImageStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Upload a PNG, JPEG or WebP image of at most 2 MB")
    @ApiResponse(responseCode = "201", description = "Image stored", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "The file is not a PNG, JPEG or WebP image")
    @ApiResponse(responseCode = "413", description = "The file is larger than 2 MB")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Secured("MANAGE_ITEMS")
    public UploadedImageDto upload(@RequestParam("file") MultipartFile file) {
        try {
            return new UploadedImageDto(imageStorage.store(file.getBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file", e);
        }
    }

    @GetMapping(path = "/{name}")
    @Operation(summary = "Read an image; its name never changes, so it can be cached for a year")
    @ApiResponse(responseCode = "404", description = "No such image")
    public ResponseEntity<byte[]> read(@PathVariable String name) {
        StoredImage image = imageStorage.read(name);
        return ResponseEntity.ok()
                .contentType(image.mediaType())
                .cacheControl(CacheControl.maxAge(CACHE_DURATION).cachePublic().immutable())
                .body(image.content());
    }
}
