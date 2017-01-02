package Main;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import java.io.*;
import java.util.*;

public class Controller {
    public static void main(String[] args) throws TwitterException, IOException {
        // 設定のロード
        Properties p = loadProperties("./conf/yy.properties");
        String[] queries = p.getProperty("list.keyword").split(",", 0);

        // ツイートの取得範囲の生成
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date end = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date begin = calendar.getTime();

        //結果格納ディレクトリの生成
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DATE);
        String directoryPath = p.getProperty("dir.name") + "/" + year + "-" + month + "-" + day;
        crateDirectory(directoryPath);

        //検索実行
        Twitter twitter = new TwitterFactory().getInstance();
        GetSearchResult gsr = new GetSearchResult(twitter, queries, directoryPath, begin, end);
        gsr.generateQuery();
        gsr.readSearchResult();
    }

    /**
     * 結果格納ディレクトリの生成
     * @param directoryPath ディレクトリパス
     */
    private static void crateDirectory(String directoryPath) {
        File dir = new File(directoryPath);
        dir.mkdir();
    }

    /**
     * 設定ファイルの読み込み
     * @param propertiesFile 設定ファイルパス
     * @return propertiesインスタンス
     * @throws IOException
     */
    private static Properties loadProperties(String propertiesFile) throws IOException {
        Properties p = new Properties();
        p.load(new BufferedReader(new FileReader(new File(propertiesFile))));
        return p;
    }

}
