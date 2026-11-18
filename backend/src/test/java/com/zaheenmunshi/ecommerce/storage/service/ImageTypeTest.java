package com.zaheenmunshi.ecommerce.storage.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ImageTypeTest {

	@ParameterizedTest
	@CsvSource({ "FFD8FFE0, JPEG", "89504E470D0A1A0A, PNG", "47494638, GIF", "524946460000000057454250, WEBP" })
	void detectsImagesByTheirMagicBytes(String hex, ImageType expected) {
		assertThat(ImageType.detect(java.util.HexFormat.of().parseHex(hex))).contains(expected);
	}

	@ParameterizedTest
	@CsvSource({ "3C68746D6C3E", "25504446", "00" })
	void rejectsEverythingElse(String hex) {
		assertThat(ImageType.detect(java.util.HexFormat.of().parseHex(hex))).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({ "../../etc/passwd", "a.png", "0f8fad5b-d9cb-469f-a165-70867728950e.exe" })
	void onlyGeneratedFilenamesAreServed(String filename) {
		assertThat(ImageType.FILENAME.matcher(filename).matches()).isFalse();
		assertThat(ImageType.FILENAME.matcher(ImageType.PNG.newFilename()).matches()).isTrue();
	}

}
