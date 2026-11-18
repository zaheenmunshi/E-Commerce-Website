package com.zaheenmunshi.ecommerce.storage.service;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;

/**
 * Image formats we accept, recognised by their leading bytes rather than by the
 * (client-controlled) file name or Content-Type header.
 */
public enum ImageType {

	JPEG("jpg", MediaType.IMAGE_JPEG), PNG("png", MediaType.IMAGE_PNG), GIF("gif", MediaType.IMAGE_GIF),
	WEBP("webp", MediaType.parseMediaType("image/webp"));

	/** Names produced by {@link #newFilename()}; anything else is never served. */
	public static final Pattern FILENAME = Pattern.compile("^[0-9a-f-]{36}\\.(jpg|png|gif|webp)$");

	private final String extension;

	private final MediaType mediaType;

	ImageType(String extension, MediaType mediaType) {
		this.extension = extension;
		this.mediaType = mediaType;
	}

	public String newFilename() {
		return UUID.randomUUID() + "." + extension;
	}

	public MediaType mediaType() {
		return mediaType;
	}

	public static Optional<ImageType> detect(byte[] bytes) {
		if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
			return Optional.of(JPEG);
		}
		if (startsWith(bytes, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
			return Optional.of(PNG);
		}
		if (startsWith(bytes, 'G', 'I', 'F', '8')) {
			return Optional.of(GIF);
		}
		if (startsWith(bytes, 'R', 'I', 'F', 'F') && bytes.length >= 12 && bytes[8] == 'W' && bytes[9] == 'E'
				&& bytes[10] == 'B' && bytes[11] == 'P') {
			return Optional.of(WEBP);
		}
		return Optional.empty();
	}

	public static Optional<ImageType> fromFilename(String filename) {
		String extension = filename.substring(filename.lastIndexOf('.') + 1);
		return Arrays.stream(values()).filter(type -> type.extension.equals(extension)).findFirst();
	}

	private static boolean startsWith(byte[] bytes, int... prefix) {
		if (bytes.length < prefix.length) {
			return false;
		}
		for (int i = 0; i < prefix.length; i++) {
			if ((bytes[i] & 0xFF) != prefix[i]) {
				return false;
			}
		}
		return true;
	}

}
