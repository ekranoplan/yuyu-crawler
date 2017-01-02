package Main;

import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.util.Map;

class RateLimit {
    private Map<String, RateLimitStatus> map;
    private String type;
    RateLimit(Twitter twitter, String type) {
        this.type=type;
        try {
            map = twitter.getRateLimitStatus();
        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }
    RateLimitStatus getRateLimit() {
        return map.get(type);
    }
}
