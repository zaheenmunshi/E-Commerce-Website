package com.zaheenmunshi.ecommerce.storage.local;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.zaheenmunshi.ecommerce.config.AppProperties;
import com.zaheenmunshi.ecommerce.storage.service.ImageStorage;
import com.zaheenmunshi.ecommerce.storage.service.ImageType;

/** Keeps images in {@code app.storage.upload-dir} (default {@code backend/data/uploads}). */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage {

	private final Path root;

	public LocalImageStorage(AppProperties properties) throws IOException {
		this.root = Files.createDirectories(properties.storage().root());
	}

	@Override
	public String store(String filename, ImageType type, byte[] content) {
		try {
			Files.write(root.resolve(filename), content);
			return PUBLIC_PREFIX + filename;
		}
		catch (IOException ex) {
			throw new UncheckedIOException("could not save image " + filename, ex);
		}
	}

	@Override
	public Optional<StoredImage> load(String filename) {
		Optional<ImageType> type = ImageType.fromFilename(filename);
		Path file = root.resolve(filename);
		if (type.isEmpty() || !Files.isRegularFile(file)) {
			return Optional.empty();
		}
		try {
			return Optional.of(new StoredImage(Files.readAllBytes(file), type.get()));
		}
		catch (IOException ex) {
			throw new UncheckedIOException("could not read image " + filename, ex);
		}
	}

}
