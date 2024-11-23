import java.util.*;

// 카테고리 목록 설정
public class WordCategory {
    private String categoryName;
    private List<String> words;

    public WordCategory(String categoryName, List<String> words) {
        this.categoryName = categoryName;
        this.words = new ArrayList<>(words); 
    }

    public String getCategoryName() {
        return categoryName;
    }

    public List<String> getWords() {
        return words;
    }

    public static List<WordCategory> initializeCategories() {
       List<WordCategory> categories = new ArrayList<>();
       categories.add(new WordCategory("동물", Arrays.asList("호랑이", "사자", "코끼리", "기린", "토끼", "고양이", "강아지", "거북이", "뱀", "표범", "치타", "하이에나", "코뿔소", "하마", "악어", "펭귄", "부엉이", "올빼미", "곰", "돼지", "소", "닭", "독수리", "타조", "고릴라", "오랑우탄", "침팬지", "원숭이", "코알라", "캥거루", "고래", "상어", "직박구리", "쥐", "청설모", "앵무새", "판다", "오소리", "오리", "백조", "두루미", "고슴도치", "너구리", "개구리", "카멜레온", "이구아나", "노루", "수달", "염소")));
       categories.add(new WordCategory("음식", Arrays.asList("김치", "불고기", "비빔밥", "된장찌개", "떡볶이", "삼겹살", "갈비찜", "잡채", "김밥", "라면", "순두부찌개", "갈비탕", "육개장", "냉면", "칼국수", "콩나물국밥", "해물파전", "족발", "보쌈", "순대국", "설렁탕", "찜닭", "닭갈비", "양념치킨", "후라이드치킨", "감자탕", "곱창구이", "막창구이", "오징어볶음", "낙지볶음", "제육볶음", "떡국", "만두국", "콩국수", "동태찌개", "부대찌개", "아구찜", "생선구이", "조기찜", "장어구이", "게장", "호박전", "감자전", "비지찌개", "멸치볶음", "진미채볶음", "오징어채볶음", "깍두기", "열무김치")));
       categories.add(new WordCategory("나라", Arrays.asList("대한민국", "미국", "중국", "일본", "러시아", "독일", "프랑스", "영국", "캐나다", "호주", "인도", "브라질", "멕시코", "이탈리아", "스페인", "네덜란드", "스웨덴", "노르웨이", "핀란드", "덴마크", "아르헨티나", "칠레", "남아프리카공화국", "이집트", "사우디아라비아", "이스라엘", "터키", "그리스", "폴란드", "체코", "헝가리", "오스트리아", "벨기에", "스위스", "포르투갈", "말레이시아", "싱가포르", "인도네시아", "태국", "베트남", "필리핀", "뉴질랜드", "몽골", "카자흐스탄", "우즈베키스탄", "파키스탄", "방글라데시", "이란", "이라크")));
       categories.add(new WordCategory("스포츠", Arrays.asList("축구", "농구", "야구", "배구", "테니스", "탁구", "크리켓", "필드하키", "아이스하키", "럭비", "골프", "복싱", "레슬링", "유도", "태권도", "펜싱", "양궁", "승마", "수영", "다이빙", "싱크로나이즈드 스위밍", "조정", "카누", "사이클링", "스피드 스케이팅", "피겨 스케이팅", "쇼트트랙 스피드 스케이팅", "알파인 스키", "노르딕 스키", "프리스타일 스키", "스노보드", "세팍타크로", "킨볼", "얼티밋 프리스비", "볼링", "배드민턴", "스쿼시", "테니스", "체조", "역도", "철인 3종 경기", "모터스포츠", "서핑", "윈드서핑", "패들보딩", "씨름", "검도", "팔씨름", "택견", "크로스핏")));

       return categories;
   }

    // 중복 단어 제외
   public String getRandomWord() { 
       if (words.isEmpty()) return null; 
       Random random = new Random();
       int index = random.nextInt(words.size());
       return words.remove(index); 
   }
}