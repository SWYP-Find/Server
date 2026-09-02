package com.swyp.picke.domain.ad.link;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AffiliateLinkResolver {

    private final Map<AdNetwork, AffiliateLinkBuilder> builders = new EnumMap<>(AdNetwork.class);

    public AffiliateLinkResolver(List<AffiliateLinkBuilder> builderList) {
        builderList.forEach(builder -> builders.put(builder.network(), builder));
    }

    public String resolve(AdCreative creative) {
        AffiliateLinkBuilder builder = builders.get(creative.getNetwork());
        if (builder == null) {
            return creative.getLandingUrl();
        }
        return builder.build(creative);
    }
}
