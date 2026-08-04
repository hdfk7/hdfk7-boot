package cn.hdfk7.boot.starter.discovery.properties;

import cn.hdfk7.boot.starter.discovery.service.NacosServiceLookup;
import com.scalar.maven.core.config.ScalarSource;
import com.scalar.maven.webflux.SpringBootScalarProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class DiscoveryScalarProperties extends SpringBootScalarProperties {
    protected final DiscoveryClient discoveryClient;
    protected final NacosServiceLookup nacosServiceLookup;
    protected final String applicationName;
    protected final List<String> excludedServices;
    protected final boolean excludeSelf;
    protected final String apiDocsPath;
    private Discovery discovery = new Discovery();

    public DiscoveryScalarProperties(DiscoveryClient discoveryClient,
                                     NacosServiceLookup nacosServiceLookup,
                                     String applicationName,
                                     List<String> excludedServices,
                                     boolean excludeSelf,
                                     String apiDocsPath) {
        this.discoveryClient = discoveryClient;
        this.nacosServiceLookup = nacosServiceLookup;
        this.applicationName = applicationName;
        this.excludedServices = excludedServices;
        this.excludeSelf = excludeSelf;
        this.apiDocsPath = apiDocsPath;
    }

    @Override
    public List<ScalarSource> getSources() {
        List<ScalarSource> sources = new ArrayList<>();
        List<ScalarSource> configuredSources = super.getSources();
        if (configuredSources != null) {
            sources.addAll(configuredSources);
        }

        List<String> serviceIds = this.serviceIds(excludedServiceIds());
        for (String serviceId : serviceIds) {
            sources.add(scalarSource(serviceId, false));
        }
        if (!sources.isEmpty() && sources.stream().noneMatch(source -> Boolean.TRUE.equals(source.isDefault()))) {
            sources.getFirst().setDefault(true);
        }
        return List.copyOf(sources);
    }

    protected List<String> serviceIds(Set<String> excludedServiceIds) {
        return discoveryClient.getServices()
                .stream()
                .filter(StringUtils::hasText)
                .filter(serviceId -> !excludedServiceIds.contains(serviceId.toLowerCase(Locale.ROOT)))
                .filter(nacosServiceLookup::hasHealthyInstances)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    @Override
    public String getUrl() {
        return this.getSources().stream()
                .filter(source -> Boolean.TRUE.equals(source.isDefault()))
                .map(ScalarSource::getUrl)
                .findFirst()
                .orElseGet(super::getUrl);
    }

    protected Set<String> excludedServiceIds() {
        Set<String> serviceIds = excludedServices.stream()
                .filter(StringUtils::hasText)
                .map(serviceId -> serviceId.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (excludeSelf && StringUtils.hasText(applicationName)) {
            serviceIds.add(applicationName.toLowerCase(Locale.ROOT));
        }
        return serviceIds;
    }

    protected ScalarSource scalarSource(String serviceId, boolean isDefault) {
        ScalarSource source = new ScalarSource();
        source.setTitle(serviceId);
        source.setSlug(slug(serviceId));
        source.setUrl("/" + serviceId + apiDocsPath());
        source.setDefault(isDefault);
        return source;
    }

    protected String apiDocsPath() {
        if (!StringUtils.hasText(apiDocsPath)) {
            return "";
        }
        return apiDocsPath.startsWith("/") ? apiDocsPath : "/" + apiDocsPath;
    }

    protected String slug(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    @Getter
    @Setter
    public static class Discovery {
        private boolean enabled = false;
    }
}
