package lotto;

import camp.nextstep.edu.missionutils.Console;
import camp.nextstep.edu.missionutils.Randoms;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Application {

    private static final int LOTTO_PRICE = 1000;
    private static final int LOTTO_MIN_NUMBER = 1;
    private static final int LOTTO_MAX_NUMBER = 45;
    private static final int LOTTO_NUMBER_COUNT = 6;
    private static final String ERROR_PREFIX = "[ERROR] ";
    private static final String COMMA_DELIMITITER = ",";

    enum Rank {
        FIRST(6, 2_000_000_000L, "6개 일치"),
        SECOND(5, 30_000_000L, "5개 일치, 보너스 볼 일치"),
        THIRD(5, 1_500_000L, "5개 일치"),
        FOURTH(4, 50_000L, "4개 일치"),
        FIFTH(3, 5_000L, "3개 일치"),
        MISS(0, 0L, "낙첨");

        private final int matchCount;
        private final long prizeMoney;
        private final String description;

        Rank(int matchCount, long prizeMoney, String description) {
            this.matchCount = matchCount;
            this.prizeMoney = prizeMoney;
            this.description = description;
        }

        public long getPrizeMoney() { return prizeMoney; }
        public String getDescription() { return description; }

        public static Rank valueOf(int matchCount, boolean bonusMatch) {
            if (matchCount == 6) { return FIRST; }
            if (matchCount == 5 && bonusMatch) { return SECOND; }
            if (matchCount == 5) { return THIRD; }
            if (matchCount == 4) { return FOURTH; }
            if (matchCount == 3) { return FIFTH; }
            return MISS;
        }
    }

    public static void main(String[] args) {
        runLottoGame();
    }

    private static void runLottoGame() {
        int purchaseAmount = readPurchaseAmount();
        int lottoCount = purchaseAmount / LOTTO_PRICE;
        System.out.printf("\n%d개를 구매했습니다.\n", lottoCount);

        List<Lotto> purchasedLottos = purchaseLottos(lottoCount);
        printPurchasedLottos(purchasedLottos);

        Lotto winningLotto = readWinningNumbers();
        int bonusNumber = readBonusNumber(winningLotto);

        Map<Rank, Integer> results = calculateResults(purchasedLottos, winningLotto, bonusNumber);
        double profitRate = calculateProfitRate(results, purchaseAmount);

        printStatistics(results, profitRate);
    }

    private static int readPurchaseAmount() {
        System.out.println("구입금액을 입력해 주세요.");
        while (true) {
            try {
                String input = Console.readLine().trim();
                return validatePurchaseAmount(input);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static int validatePurchaseAmount(String input) {
        int amount = parseNumeric(input, "구입 금액은 숫자여야 합니다.");
        if (amount <= 0 || amount % LOTTO_PRICE != 0) {
            throw new IllegalArgumentException(ERROR_PREFIX + "구입 금액은 1,000원 단위여야 합니다.");
        }
        return amount;
    }

    private static List<Lotto> purchaseLottos(int lottoCount) {
        List<Lotto> lottos = new ArrayList<>();
        for (int i = 0; i < lottoCount; i++) {
            lottos.add(generateLotto());
        }
        return lottos;
    }

    private static Lotto generateLotto() {
        List<Integer> numbers = Randoms.pickUniqueNumbersInRange(
                LOTTO_MIN_NUMBER,
                LOTTO_MAX_NUMBER,
                LOTTO_NUMBER_COUNT
        );
        return new Lotto(numbers);
    }

    private static void printPurchasedLottos(List<Lotto> lottos) {
        for (Lotto lotto : lottos) {
            System.out.println(lotto.getSortedNumbers());
        }
    }

    private static Lotto readWinningNumbers() {
        System.out.println("\n당첨 번호를 입력해 주세요.");
        while (true) {
            try {
                String input = Console.readLine().trim();
                List<Integer> numbers = parseWinningNumbers(input);
                return new Lotto(numbers); // Lotto 생성자에서 유효성 검사
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static List<Integer> parseWinningNumbers(String input) {
        try {
            return Arrays.stream(input.split(COMMA_DELIMITITER))
                    .map(String::trim)
                    .map(s -> parseNumeric(s, "당첨 번호는 숫자여야 합니다."))
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(ERROR_PREFIX + "당첨 번호 형식이 올바르지 않습니다.");
        }
    }

    private static int readBonusNumber(Lotto winningLotto) {
        System.out.println("\n보너스 번호를 입력해 주세요.");
        while (true) {
            try {
                String input = Console.readLine();
                return validateBonusNumber(winningLotto, input);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static int validateBonusNumber(Lotto winningLotto, String input) {
        int bonusNumber = parseNumeric(input, "보너스 번호는 숫자여야 합니다.");
        if (bonusNumber < LOTTO_MIN_NUMBER || bonusNumber > LOTTO_MAX_NUMBER) {
            throw new IllegalArgumentException(ERROR_PREFIX + "보너스 번호는 1부터 45 사이여야 합니다.");
        }
        if (winningLotto.contains(bonusNumber)) {
            throw new IllegalArgumentException(ERROR_PREFIX + "보너스 번호는 당첨 번호와 중복될 수 없습니다.");
        }
        return bonusNumber;
    }

    private static Map<Rank, Integer> calculateResults(List<Lotto> lottos, Lotto winning, int bonus) {
        Map<Rank, Integer> results = new EnumMap<>(Rank.class);
        Arrays.stream(Rank.values()).forEach(rank -> results.put(rank, 0));

        for (Lotto lotto : lottos) {
            Rank rank = determineRank(lotto, winning, bonus);
            results.put(rank, results.get(rank) + 1);
        }
        return results;
    }

    private static Rank determineRank(Lotto lotto, Lotto winning, int bonus) {
        int matchCount = lotto.countMatch(winning);
        boolean bonusMatch = lotto.contains(bonus);
        return Rank.valueOf(matchCount, bonusMatch);
    }

    private static double calculateProfitRate(Map<Rank, Integer> results, int purchaseAmount) {
        long totalPrize = 0;
        for (Rank rank : results.keySet()) {
            totalPrize += rank.getPrizeMoney() * results.get(rank);
        }
        double rate = (double) totalPrize / purchaseAmount * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }

    private static void printStatistics(Map<Rank, Integer> results, double profitRate) {
        System.out.println("\n당첨 통계");
        System.out.println("---");

        DecimalFormat prizeFormat = new DecimalFormat("#,###");
        Rank[] ranksToDisplay = {Rank.FIFTH, Rank.FOURTH, Rank.THIRD, Rank.SECOND, Rank.FIRST};

        for (Rank rank : ranksToDisplay) {
            System.out.printf("%s (%,d원) - %d개\n",
                    rank.getDescription(),
                    rank.getPrizeMoney(),
                    results.get(rank));
        }

        DecimalFormat rateFormat = new DecimalFormat("#,##0.0");
        System.out.printf("총 수익률은 %s%%입니다.\n", rateFormat.format(profitRate));
    }

    private static int parseNumeric(String input, String errorMessage) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ERROR_PREFIX + errorMessage);
        }
    }
}