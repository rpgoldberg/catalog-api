package com.catalogcollector.service;

import com.catalogcollector.config.ProviderProperties;
import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.entity.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Order(4)
public class MusicBrainzProvider implements CatalogLookupProvider {

    private static final Logger log = LoggerFactory.getLogger(MusicBrainzProvider.class);
    private static final Set<MediaType> SUPPORTED = Set.of(MediaType.OTHER);

    private final String baseUrl;
    private final String contactEmail;
    private final RestClient restClient;

    public MusicBrainzProvider(ProviderProperties props, RestClient restClient) {
        this.baseUrl = props.musicBrainz().baseUrl();
        this.contactEmail = props.musicBrainz().contactEmail();
        this.restClient = restClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogLookupResult> lookupByBarcode(String barcode, String barcodeType) {
        if (contactEmail == null || contactEmail.isBlank()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri(baseUrl + "/ws/2/release?query=barcode:{barcode}&fmt=json&limit=1", barcode)
                    .header("User-Agent", "CatalogCollector/1.0.0 (" + contactEmail + ")")
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return Optional.empty();
            }

            List<Map<String, Object>> releases = (List<Map<String, Object>>) response.get("releases");
            if (releases == null || releases.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> release = releases.getFirst();
            String title = (String) release.get("title");
            String publisher = extractArtist(release);
            String releaseDate = (String) release.get("date");
            String mbid = (String) release.get("id");
            String coverUrl = "https://coverartarchive.org/release/" + mbid + "/front-500";
            String format = extractFormat(release);

            Map<String, Object> metadata = null;
            if (format != null) {
                metadata = new HashMap<>();
                metadata.put("format", format);
            }

            return Optional.of(new CatalogLookupResult(
                    title, publisher, null, coverUrl, null, null, releaseDate, metadata));
        } catch (Exception e) {
            log.warn("MusicBrainz lookup failed for barcode {}: {}", barcode, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getSourceName() {
        return "MusicBrainz";
    }

    @Override
    public Set<MediaType> getSupportedMediaTypes() {
        return SUPPORTED;
    }

    @SuppressWarnings("unchecked")
    private String extractArtist(Map<String, Object> release) {
        List<Map<String, Object>> artistCredit = (List<Map<String, Object>>) release.get("artist-credit");
        if (artistCredit != null && !artistCredit.isEmpty()) {
            return (String) artistCredit.getFirst().get("name");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractFormat(Map<String, Object> release) {
        List<Map<String, Object>> media = (List<Map<String, Object>>) release.get("media");
        if (media != null && !media.isEmpty()) {
            return (String) media.getFirst().get("format");
        }
        return null;
    }
}
