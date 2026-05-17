package com.swyp.picke.domain.oauth.dto.apple;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ApplePublicKeyResponse {
    private List<Key> keys;

    @Getter
    @Setter
    public static class Key {
        private String kty;
        private String kid;
        private String use;
        private String alg;
        private String n;
        private String e;
    }
}