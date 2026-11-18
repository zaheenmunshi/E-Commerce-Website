package com.zaheenmunshi.ecommerce.storage.s3;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.zaheenmunshi.ecommerce.config.AppProperties;
import com.zaheenmunshi.ecommerce.storage.service.ImageStorage;
import com.zaheenmunshi.ecommerce.storage.service.ImageType;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Keeps images in a private S3 bucket ({@code app.storage.s3-bucket}). Region and
 * credentials come from the AWS default chain, i.e. the ECS task role on AWS.
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3ImageStorage implements ImageStorage {

	private static final String KEY_PREFIX = "products/";

	private final S3Client s3;

	private final String bucket;

	public S3ImageStorage(AppProperties properties) {
		this.s3 = S3Client.create();
		this.bucket = properties.storage().s3Bucket();
	}

	@Override
	public String store(String filename, ImageType type, byte[] content) {
		s3.putObject(request -> request.bucket(bucket)
			.key(KEY_PREFIX + filename)
			.contentType(type.mediaType().toString()), RequestBody.fromBytes(content));
		return PUBLIC_PREFIX + filename;
	}

	@Override
	public Optional<StoredImage> load(String filename) {
		Optional<ImageType> type = ImageType.fromFilename(filename);
		if (type.isEmpty()) {
			return Optional.empty();
		}
		try {
			byte[] content = s3.getObjectAsBytes(request -> request.bucket(bucket).key(KEY_PREFIX + filename))
				.asByteArray();
			return Optional.of(new StoredImage(content, type.get()));
		}
		catch (NoSuchKeyException ex) {
			return Optional.empty();
		}
	}

}
