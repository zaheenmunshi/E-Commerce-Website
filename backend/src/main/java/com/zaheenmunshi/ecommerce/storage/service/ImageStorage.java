package com.zaheenmunshi.ecommerce.storage.service;

import java.util.Optional;

import com.zaheenmunshi.ecommerce.storage.web.UploadController;

/**
 * Stores product images. Pick the implementation with {@code app.storage.type}
 * ({@code local} or {@code s3}); callers only ever see public paths like
 * {@code /uploads/<uuid>.jpg}, which {@link UploadController} serves.
 */
public interface ImageStorage {

	String PUBLIC_PREFIX = "/uploads/";

	/**
	 * @param filename a generated name, see {@link ImageType#newFilename()}
	 * @return the public path of the stored image
	 */
	String store(String filename, ImageType type, byte[] content);

	Optional<StoredImage> load(String filename);

	record StoredImage(byte[] content, ImageType type) {
	}

}
