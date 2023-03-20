package net.socketconnection.jva;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.socketconnection.jva.enums.Language;
import net.socketconnection.jva.enums.Region;
import net.socketconnection.jva.exceptions.IncorrectDataException;
import net.socketconnection.jva.exceptions.InvalidAuthenticationException;
import net.socketconnection.jva.exceptions.InvalidRiotIdentificationException;
import net.socketconnection.jva.exceptions.RateLimitedException;
import net.socketconnection.jva.models.Version;
import net.socketconnection.jva.models.WebsiteArticle;
import net.socketconnection.jva.models.shop.Bundle;
import net.socketconnection.jva.models.shop.ContentTier;
import net.socketconnection.jva.models.shop.item.BundleItem;
import net.socketconnection.jva.models.shop.item.Item;
import net.socketconnection.jva.models.shop.item.OfferItem;
import net.socketconnection.jva.models.status.ServerStatus;
import net.socketconnection.jva.models.status.StatusEntry;
import net.socketconnection.jva.models.status.Update;
import net.socketconnection.jva.player.LeaderboardPlayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ValorantAPI {

    private final URL baseUrl;
    private final String apiKey;

    public ValorantAPI(String apiKey) throws IOException {
        baseUrl = new URL("https://api.henrikdev.xyz/valorant");
        this.apiKey = apiKey;
    }

    public ValorantAPI() throws IOException {
        this(null);
    }

    public List<LeaderboardPlayer> getLeaderboard(Region region, String riotId) throws IOException {
        JsonArray leaderboardData;

        if(riotId == null) {
            leaderboardData = sendRestRequest("/v1/leaderboard/" + region.getQuery()).getAsJsonArray();
        } else {
            if(!riotId.contains("#") || riotId.split("#").length < 2) {
                throw new InvalidRiotIdentificationException("Unknown format (right format: NAME#TAG)");
            }

            String[] data = riotId.split("#");

            leaderboardData = sendRestRequest("/v1/leaderboard/" + region.getQuery() + "?name=" + data[0] + "&tag=" + data[1]).getAsJsonArray();
        }

        List<LeaderboardPlayer> leaderboard = new LinkedList<>();

        for(JsonElement element : leaderboardData) {
            leaderboard.add(new LeaderboardPlayer(this).fetchData(element.getAsJsonObject()));
        }

        return leaderboard;
    }

    public List<LeaderboardPlayer> getLeaderboard(Region region) throws IOException {
        return getLeaderboard(region, null);
    }

    public ServerStatus getServerStatus(Region region) throws IOException {
        JsonObject statusData = sendRestRequest("/v1/status/" + region.getQuery()).getAsJsonObject().getAsJsonObject("data");

        JsonArray maintenancesData = statusData.getAsJsonArray("maintenances");
        JsonArray incidentsData = statusData.getAsJsonArray("incidents");

        List<StatusEntry> maintenances = new LinkedList<>();
        List<StatusEntry> incidents = new LinkedList<>();

        for(JsonElement maintenanceElement : maintenancesData) {
            JsonObject maintenanceObject = maintenanceElement.getAsJsonObject();
            List<Update> updates = new LinkedList<>();

            for(JsonElement updateElement : maintenanceObject.getAsJsonArray("updates")) {
                JsonObject updateObject = updateElement.getAsJsonObject();
                Map<Language, String> translations = new LinkedHashMap<>();

                for(JsonElement translationElement : updateObject.getAsJsonArray("translations")) {
                    JsonObject translationObject = translationElement.getAsJsonObject();

                    translations.put(Language.getFromLocale(translationObject.get("locale").getAsString()), translationObject.get("content").getAsString());
                }

                List<String> publishLocations = new LinkedList<>();

                for(JsonElement publishElement : updateObject.getAsJsonArray("publish_locations")) {
                    publishLocations.add(publishElement.getAsString());
                }

                String createdAt = null;
                String updatedAt = null;

                if(!updateObject.get("created_at").isJsonNull()) {
                    createdAt = updateObject.get("created_at").getAsString();
                }

                if(!updateObject.get("updated_at").isJsonNull()) {
                    updatedAt = updateObject.get("updated_at").getAsString();
                }

                updates.add(new Update(createdAt, updatedAt,
                        updateObject.get("publish").getAsBoolean(), updateObject.get("id").getAsInt(), translations,
                        publishLocations.toArray(new String[0]), updateObject.get("author").getAsString()));
            }

            List<String> platforms = new LinkedList<>();

            for(JsonElement platformElement : maintenanceObject.getAsJsonArray("platforms")) {
                platforms.add(platformElement.getAsString());
            }

            Map<Language, String> titles = new LinkedHashMap<>();

            for(JsonElement titleElement : maintenanceObject.getAsJsonArray("titles")) {
                JsonObject titleObject = titleElement.getAsJsonObject();

                titles.put(Language.getFromLocale(titleObject.get("locale").getAsString()), titleObject.get("content").getAsString());
            }

            String createdAt = null;
            String archiveAt = null;
            String updatedAt = null;
            String maintenanceStatus = null;
            String incidentSeverity = null;

            if(!maintenanceObject.get("created_at").isJsonNull()) {
                createdAt = maintenanceObject.get("created_at").getAsString();
            }

            if(!maintenanceObject.get("archive_at").isJsonNull()) {
                archiveAt = maintenanceObject.get("archive_at").getAsString();
            }

            if(!maintenanceObject.get("updated_at").isJsonNull()) {
                updatedAt = maintenanceObject.get("updated_at").getAsString();
            }

            if(!maintenanceObject.get("maintenance_status").isJsonNull()) {
                maintenanceStatus = maintenanceObject.get("maintenance_status").getAsString();
            }

            if(!maintenanceObject.get("incident_severity").isJsonNull()) {
                incidentSeverity = maintenanceObject.get("incident_severity").getAsString();
            }

            maintenances.add(new StatusEntry(createdAt, archiveAt, updates.toArray(new Update[0]), platforms.toArray(new String[0]),
                    updatedAt, maintenanceObject.get("id").getAsInt(), titles, maintenanceStatus, incidentSeverity));
        }

        for(JsonElement incidentElement : incidentsData) {
            JsonObject incidentObject = incidentElement.getAsJsonObject();
            List<Update> updates = new LinkedList<>();

            for(JsonElement updateElement : incidentObject.getAsJsonArray("updates")) {
                JsonObject updateObject = updateElement.getAsJsonObject();
                Map<Language, String> translations = new LinkedHashMap<>();

                for(JsonElement translationElement : updateObject.getAsJsonArray("translations")) {
                    JsonObject translationObject = translationElement.getAsJsonObject();

                    translations.put(Language.getFromLocale(translationObject.get("locale").getAsString()), translationObject.get("content").getAsString());
                }

                List<String> publishLocations = new LinkedList<>();

                for(JsonElement publishElement : updateObject.getAsJsonArray("publish_locations")) {
                    publishLocations.add(publishElement.getAsString());
                }

                String createdAt = null;
                String updatedAt = null;

                if(!updateObject.get("created_at").isJsonNull()) {
                    createdAt = updateObject.get("created_at").getAsString();
                }

                if(!updateObject.get("updated_at").isJsonNull()) {
                    updatedAt = updateObject.get("updated_at").getAsString();
                }

                updates.add(new Update(createdAt, updatedAt,
                        updateObject.get("publish").getAsBoolean(), updateObject.get("id").getAsInt(), translations,
                        publishLocations.toArray(new String[0]), updateObject.get("author").getAsString()));
            }

            List<String> platforms = new LinkedList<>();

            for(JsonElement platformElement : incidentObject.getAsJsonArray("platforms")) {
                platforms.add(platformElement.getAsString());
            }

            Map<Language, String> titles = new LinkedHashMap<>();

            for(JsonElement titleElement : incidentObject.getAsJsonArray("titles")) {
                JsonObject titleObject = titleElement.getAsJsonObject();

                titles.put(Language.getFromLocale(titleObject.get("locale").getAsString()), titleObject.get("content").getAsString());
            }

            String createdAt = null;
            String archiveAt = null;
            String updatedAt = null;
            String maintenanceStatus = null;
            String incidentSeverity = null;

            if(!incidentObject.get("created_at").isJsonNull()) {
                createdAt = incidentObject.get("created_at").getAsString();
            }

            if(!incidentObject.get("archive_at").isJsonNull()) {
                archiveAt = incidentObject.get("archive_at").getAsString();
            }

            if(!incidentObject.get("updated_at").isJsonNull()) {
                updatedAt = incidentObject.get("updated_at").getAsString();
            }

            if(!incidentObject.get("maintenance_status").isJsonNull()) {
                maintenanceStatus = incidentObject.get("maintenance_status").getAsString();
            }

            if(!incidentObject.get("incident_severity").isJsonNull()) {
                incidentSeverity = incidentObject.get("incident_severity").getAsString();
            }

            incidents.add(new StatusEntry(createdAt, archiveAt, updates.toArray(new Update[0]), platforms.toArray(new String[0]),
                    updatedAt, incidentObject.get("id").getAsInt(), titles, maintenanceStatus, incidentSeverity));
        }

        return new ServerStatus(maintenances.toArray(new StatusEntry[0]), incidents.toArray(new StatusEntry[0]));
    }

    public Version getVersion(Region region) throws IOException {
        JsonObject versionData = sendRestRequest("/v1/version/" + region.getQuery()).getAsJsonObject().getAsJsonObject("data");

        return new Version(versionData.get("version").getAsString(), versionData.get("clientVersion").getAsString(),
                versionData.get("branch").getAsString(), Region.getFromQuery(versionData.get("region").getAsString()));
    }

    public List<WebsiteArticle> getWebsiteArticles(Language language) throws IOException {
        JsonArray articleData = sendRestRequest("/v1/website/" + language.getLocaleUrl()).getAsJsonObject().getAsJsonArray("data");

        List<WebsiteArticle> websiteArticles = new LinkedList<>();

        for(JsonElement articleElement : articleData) {
            JsonObject articleObject = articleElement.getAsJsonObject();

            String externalLink = null;

            if(!articleObject.get("external_link").isJsonNull()) {
                externalLink = articleObject.get("external_link").getAsString();
            }

            websiteArticles.add(new WebsiteArticle(articleObject.get("banner_url").getAsString(), WebsiteArticle.Category.getFromQuery(articleObject.get("category").getAsString()),
                    articleObject.get("date").getAsString(), externalLink, articleObject.get("title").getAsString(), articleObject.get("url").getAsString()));
        }

        return websiteArticles;
    }

    public List<Bundle> getStoreBundles() throws IOException {
        JsonArray bundlesData = sendRestRequest("/v2/store-featured").getAsJsonObject().getAsJsonArray("data");

        List<Bundle> bundles = new LinkedList<>();

        for(JsonElement bundleElement : bundlesData) {
            JsonObject bundleObject = bundleElement.getAsJsonObject();
            JsonArray itemData = bundleObject.getAsJsonArray("items");

            List<BundleItem> items = new LinkedList<>();

            for(JsonElement itemElement : itemData) {
                JsonObject itemObject = itemElement.getAsJsonObject();

                items.add(new BundleItem(itemObject.get("uuid").getAsString(), itemObject.get("name").getAsString(),
                        itemObject.get("image").getAsString(), Item.Type.getFromQuery(itemObject.get("type").getAsString()),
                        itemObject.get("amount").getAsInt(), itemObject.get("discount_percent").getAsInt(), itemObject.get("base_price").getAsInt(),
                        itemObject.get("discounted_price").getAsInt(), itemObject.get("promo_item").getAsBoolean()));
            }

            bundles.add(new Bundle(bundleObject.get("bundle_uuid").getAsString(), bundleObject.get("bundle_price").getAsInt(),
                    bundleObject.get("whole_sale_only").getAsBoolean(), items.toArray(new BundleItem[0]), bundleObject.get("seconds_remaining").getAsLong(),
                    bundleObject.get("expires_at").getAsString()));
        }

        return bundles;
    }

    public List<OfferItem> getStoreOffers() throws IOException {
        JsonArray offersData = sendRestRequest("/v2/store-offers").getAsJsonObject().getAsJsonObject("data").getAsJsonArray("offers");

        List<OfferItem> items = new LinkedList<>();

        for(JsonElement offerElement : offersData) {
            JsonObject offerObject = offerElement.getAsJsonObject();

            String skinId = null;
            ContentTier contentTier = null;

            if(!offerObject.get("skin_id").isJsonNull()) {
                skinId = offerObject.get("skin_id").getAsString();
            }

            if(!offerObject.get("content_tier").isJsonNull()) {
                JsonObject contentTierObject = offerObject.getAsJsonObject("content_tier");

                contentTier = new ContentTier(contentTierObject.get("name").getAsString(), contentTierObject.get("dev_name").getAsString(),
                        contentTierObject.get("icon").getAsString());
            }

            items.add(new OfferItem(skinId, offerObject.get("name").getAsString(),
                    offerObject.get("icon").getAsString(), Item.Type.getFromQuery(offerObject.get("type").getAsString()),
                    offerObject.get("offer_id").getAsString(), offerObject.get("cost").getAsInt(), contentTier));
        }

        return items;
    }

    public JsonElement sendRestRequest(String uriPath) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + uriPath).openConnection();

        if(apiKey != null) {
            connection.setRequestProperty("Authorization", apiKey);
        }

        connection.setRequestProperty("User-Agent", "Java Valorant API (JVA)");
        connection.setDoInput(true);

        switch (connection.getResponseCode()) {
            case 403:
                throw new InvalidAuthenticationException(connection.getResponseMessage());
            case 404:
                throw new IncorrectDataException(connection.getResponseMessage());
            case 429:
                throw new RateLimitedException(connection.getResponseMessage());
        }

        StringBuilder builder = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String msg;

        while ((msg = reader.readLine()) != null) {
            builder.append(msg).append("\n");
        }

        return new Gson().fromJson(builder.toString(), JsonElement.class);
    }

    public String getApiKey() {
        return apiKey;
    }

    public URL getBaseUrl() {
        return baseUrl;
    }
}
