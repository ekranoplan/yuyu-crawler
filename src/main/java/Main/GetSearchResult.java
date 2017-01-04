package Main;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

class GetSearchResult {
    private Twitter twitter;
    private Query query;
    private String[] queries;
    private String dirName;
    private Date begin;
    private Date end;

    GetSearchResult(Twitter twitter, String[] queries, String dirName, Date begin, Date end) {
        this.twitter = twitter;
        this.queries = queries;
        this.dirName = dirName;
        this.begin = begin;
        this.end = end;
        logger("INFO", "START_SEARCHING", "yuyu-crawler is started." + begin + " to " + end);
    }

    /**
     * ログ出力（標準出力）
     * @param type ファシリティ
     * @param title メッセージの種類
     * @param message メッセージ本文
     */
    private void logger(String type, String title, String message) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        System.out.println(calendar.getTime() + " [" + type + "]" + " [" + title + "] " + message);
    }

    /**
     * 引数として渡されたAPIリミットのCheckを行い、残数が少なかった場合はsleepする
     * @param rateLimit 指定サれたエンドポイントのステータス
     */
    private void checkAndSleep(RateLimitStatus rateLimit) {
        logger("INFO", "CHECK_RATE_LIMIT", String.valueOf(rateLimit.getRemaining()));
        if (rateLimit.getRemaining() < 3) {
            long l = rateLimit.getSecondsUntilReset();
            logger("INFO", "SEARCH_API_LIMIT", "SLEEP: " + l + " sec.");
            try {
                Thread.sleep(l * 1000 + 5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 引数として渡されたツイートデータをファイルに出力する
     * @param status ツイートデータ
     * @throws IOException
     */
    private void storeStatus(Status status) throws IOException {
        String rawJSON = TwitterObjectFactory.getRawJSON(status);
        if (null != rawJSON) {
            String fileName = this.dirName + "/" + status.getId() + ".json";
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(new File(fileName)), "UTF-8"));
            bw.write(rawJSON);
            bw.close();
        } else {
            logger("WORN", "JSON_NULL", status.getId() + " is NULL");
        }

    }

    /**
     * ツイートを取得する
     * @throws TwitterException
     * @throws IOException
     */
    void readSearchResult() throws TwitterException, IOException {

        QueryResult result;
        List<Status> tweetList;
        Queue<Long> timeQueue = new PriorityQueue<>();

        boolean loop =true;

        do {
            Queue<Long> q = new PriorityQueue<>();

            do {
                RateLimit rateLimit = new RateLimit(twitter, "/search/tweets");
                checkAndSleep(rateLimit.getRateLimit());

                result = twitter.search(query);
                tweetList = result.getTweets();
                logger("INFO", "NUMBER_OF_TWEET", "Getting " + tweetList.size() + " tweets");

                // 取得範囲に含まれているツイートを永続化する
                for (Status s : tweetList) {
                    Date date = s.getCreatedAt();
                    if (containsCheck(s.getText()) && date.after(this.begin) && date.before(this.end)) {
                        storeStatus(s);
                    }
                    q.add(s.getId());
                    timeQueue.add(s.getCreatedAt().getTime());
                }

                if (this.begin.getTime() > timeQueue.peek()) {
                    loop=false;
                    break;
                }

            } while ((query = result.nextQuery()) != null);

            // ツイートの取得漏れ対策のために、既に取得したツイートのうち最古のツイートのIDをMAXIDとして再度検索を実施
            generateQuery();
            improveQuery(q.peek());

        }while (loop);

        logger("INFO", "FINISH_SEARCHING", "yuyu-crawler is stopped.");


    }

    /**
     * ツイート本文にキーワードが部分一致しているか確認する
     * @param text Checkしたいツイート本文
     * @return
     */
    private boolean containsCheck(String text) {
        for (String q : queries) {
            if (text.contains(q)) {
                return true;
            }
        }
        return false;
    }

    /**
     * MAXIDの指定の更新
     * @param id 指定したいID
     */
    private void improveQuery(long id) {
        logger("INFO", "IMPROVES_QUERY", "Set maxid=" + id);
        this.query.setMaxId(id);
    }

    /**
     * クエリの生成
     */
    void generateQuery() {
        this.query = new Query();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.queries.length; i++) {
            sb.append("\"");
            sb.append(queries[i]);
            sb.append("\"");
            if (i < this.queries.length - 1) {
                sb.append(" OR ");
            }
        }
        this.query.setCount(100);
        this.query.setQuery(sb.toString());
    }
}
