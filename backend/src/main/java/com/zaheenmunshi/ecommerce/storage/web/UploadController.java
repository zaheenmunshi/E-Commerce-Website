package com.zaheenmunshi.ecommerce.storage.web;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.zaheenmunshi.ecommerce.storage.service.ImageStorage;
import com.zaheenmunshi.ecommerce.storage.service.ImageType;

/** Serves stored product images, whichever {@link ImageStorage} holds them. */
@RestController
public class UploadController {

	private final ImageStorage storage;

	public UploadController(ImageStorage storage) {
		this.storage = storage;
	}

	@GetMapping(ImageStorage.PUBLIC_PREFIX + "{filename}")
	ResponseEntity<byte[]> image(@PathVariable String filename) {
		if (!ImageType.FILENAME.matcher(filename).matches()) {
			return ResponseEntity.notFound().build();
		}
		return storage.load(filename)
			.map(image -> ResponseEntity.ok()
				.contentType(image.type().mediaType())
				// names are random and never reused, so the browser may cache them for good
				.cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
				.body(image.content()))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

}
