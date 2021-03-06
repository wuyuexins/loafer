package com.roysmond.loafer.api.service;

import com.roysmond.loafer.api.domain.User;
import com.roysmond.loafer.api.exception.AuthTokenParseException;
import com.roysmond.loafer.api.repository.UserRepository;
import com.roysmond.loafer.api.security.TokenHandler;
import com.roysmond.loafer.api.support.UserInfo;
import com.roysmond.loafer.api.utils.JacksonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.DatatypeConverter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by wuyuexin on 2017/5/23.
 */
@Service
@Transactional
public class TokenAuthenticationServiceImpl implements TokenAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(TokenAuthenticationServiceImpl.class);

    private final TokenHandler tokenHandler;

    private final UserRepository userRepository;

    private final StringRedisTemplate stringRedisTemplate;

    public TokenAuthenticationServiceImpl(UserRepository userRepository,
                                          StringRedisTemplate stringRedisTemplate) {
        this.userRepository = userRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        // TODO: parse this as a property
        tokenHandler = new TokenHandler(DatatypeConverter.parseBase64Binary("9SyECk96oDsTmXfogIfgdjhdsgvagHJLKNLvfdsfR8cbXTvoPjX+Pq/T/b1PqpHX0lYm0oCBjXWICA=="));
    }

    @Override
    public String createAuthToken(Authentication authentication) {
        log.info("{} createAuthToken start. ", authentication.getPrincipal());
        org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        UserInfo userInfo = userRepository.findOneByLogin(user.getUsername()).map(userFromDB -> {
            return new UserInfo(userFromDB.getId(), userFromDB.getUsername());
        }).orElseThrow(() -> new NullPointerException());

        String token = tokenHandler.createTokenForUser(userInfo);
        stringRedisTemplate.opsForValue().set(token, JacksonUtil.genInstance().toJacksonString(userInfo), 60, TimeUnit.MINUTES);
        log.info("{} createAuthToken end. token is {}", authentication.getPrincipal(), token);
        return token;
    }

    @Override
    public Authentication getAuthentication(String authToken) {
        if (StringUtils.isNotEmpty(authToken)) {
            String userInfoString = stringRedisTemplate.opsForValue().get(authToken);
            if (StringUtils.isNotEmpty(userInfoString)) {
                UserInfo userInfo = JacksonUtil.genInstance().getJacksonBean(userInfoString, UserInfo.class);
                Optional<User> userOptional = userRepository.findOneWithAuthoritiesByLogin(userInfo.getUsername());
                return userOptional.map(user -> {
                    List<GrantedAuthority> grantedAuthorities = user.getAuthorities().stream()
                            .map(authority -> new SimpleGrantedAuthority(authority.getName()))
                            .collect(Collectors.toList());
                    return new UsernamePasswordAuthenticationToken(user, null, grantedAuthorities);
                }).orElseThrow(() -> new AuthTokenParseException("AuthToken can't be parsed."));
            }
        }
        return null;
    }

    @Override
    public void removeAuthToken(String authToken) {
        if (StringUtils.isNotEmpty(authToken)){
            stringRedisTemplate.delete(authToken);
        }
    }
}
