package com.example.mallhome.controller.merchant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchant/uploads")
public class MerchantUploadController {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping("/product-image")
    public Map<String, Object> uploadProductImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        validateImage(file);
        String extension = extension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;
        try {
            Path productUploadDir = productUploadDir();
            Files.createDirectories(productUploadDir);
            Path target = productUploadDir.resolve(filename).toAbsolutePath().normalize();
            file.transferTo(target);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败");
        }
        String path = "/uploads/products/" + filename;
        return Map.of("url", absoluteUrl(request, path), "path", path);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片不能超过5MB");
        }
        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(String.valueOf(contentType).toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 JPG、PNG、WEBP、GIF 图片");
        }
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片格式不支持");
        }
    }

    private String extension(String filename) {
        String cleanName = StringUtils.cleanPath(filename == null ? "" : filename);
        int dot = cleanName.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return cleanName.substring(dot).toLowerCase(Locale.ROOT);
    }

    private Path productUploadDir() {
        return Path.of(uploadDir, "products").toAbsolutePath().normalize();
    }

    private String absoluteUrl(HttpServletRequest request, String path) {
        String scheme = firstHeader(request, "X-Forwarded-Proto", request.getScheme());
        String host = firstHeader(request, "X-Forwarded-Host", request.getHeader("Host"));
        if (!StringUtils.hasText(host)) {
            host = request.getServerName() + ":" + request.getServerPort();
        }
        return scheme + "://" + host + path;
    }

    private String firstHeader(HttpServletRequest request, String header, String fallback) {
        String value = request.getHeader(header);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        int comma = value.indexOf(',');
        return comma >= 0 ? value.substring(0, comma).trim() : value.trim();
    }
}
