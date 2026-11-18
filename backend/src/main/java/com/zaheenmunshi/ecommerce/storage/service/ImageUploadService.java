package com.zaheenmunshi.ecommerce.storage.service;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.zaheenmunshi.ecommerce.common.error.ApiException;

/** Validates an uploaded file is a real image, then hands it to the configured storage. */
@Service
public class ImageUploadService {

	private final ImageStorage storage;

	public ImageUploadService(ImageStorage storage) {
		this.storage = storage;
	}

	public String upload(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw ApiException.badRequest("choose an image to upload");
		}
		byte[] content = readBytes(file);
		ImageType type = ImageType.detect(content)
			.orElseThrow(() -> ApiException.badRequest("upload image only (jpg, png, gif or webp)"));
		return storage.store(type.newFilename(), type, content);
	}

	private static byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("could not read upload", ex);
		}
	}

}
