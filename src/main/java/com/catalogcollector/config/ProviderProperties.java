package com.catalogcollector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.providers")
public record ProviderProperties(
        OpenLibrary openLibrary,
        GoogleBooks googleBooks,
        UpcItemDb upcItemDb,
        BestBuy bestBuy,
        MusicBrainz musicBrainz,
        EbayBrowse ebayBrowse,
        Tmdb tmdb,
        Igdb igdb,
        AniList aniList
) {
    public record OpenLibrary(String baseUrl) {
        public OpenLibrary {
            if (baseUrl == null) baseUrl = "https://openlibrary.org";
        }
    }

    public record GoogleBooks(String baseUrl, String apiKey) {
        public GoogleBooks {
            if (baseUrl == null) baseUrl = "https://www.googleapis.com";
            if (apiKey == null) apiKey = "";
        }
    }

    public record UpcItemDb(String baseUrl) {
        public UpcItemDb {
            if (baseUrl == null) baseUrl = "https://api.upcitemdb.com";
        }
    }

    public record BestBuy(String baseUrl, String apiKey) {
        public BestBuy {
            if (baseUrl == null) baseUrl = "https://api.bestbuy.com";
            if (apiKey == null) apiKey = "";
        }
    }

    public record MusicBrainz(String baseUrl, String contactEmail) {
        public MusicBrainz {
            if (baseUrl == null) baseUrl = "https://musicbrainz.org";
            if (contactEmail == null) contactEmail = "";
        }
    }

    public record EbayBrowse(String baseUrl, String tokenUrl, String clientId, String clientSecret) {
        public EbayBrowse {
            if (baseUrl == null) baseUrl = "https://api.ebay.com";
            if (tokenUrl == null) tokenUrl = "https://api.ebay.com/identity/v1/oauth2/token";
            if (clientId == null) clientId = "";
            if (clientSecret == null) clientSecret = "";
        }
    }

    public record Tmdb(String baseUrl, String apiKey) {
        public Tmdb {
            if (baseUrl == null) baseUrl = "https://api.themoviedb.org";
            if (apiKey == null) apiKey = "";
        }
    }

    public record Igdb(String baseUrl, String twitchTokenUrl, String clientId, String clientSecret) {
        public Igdb {
            if (baseUrl == null) baseUrl = "https://api.igdb.com";
            if (twitchTokenUrl == null) twitchTokenUrl = "https://id.twitch.tv/oauth2/token";
            if (clientId == null) clientId = "";
            if (clientSecret == null) clientSecret = "";
        }
    }

    public record AniList(String baseUrl) {
        public AniList {
            if (baseUrl == null) baseUrl = "https://graphql.anilist.co";
        }
    }
}
