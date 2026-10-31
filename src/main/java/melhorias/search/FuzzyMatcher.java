package melhorias.search;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;

/** Busca aproximada local, sem dependências do Minecraft (PT-BR e EN). */
public final class FuzzyMatcher {
    private static final Set<String> STOP = Set.of("de", "da", "do", "das", "dos", "e", "of", "the", "a", "o");

    private static final java.util.regex.Pattern MARKS = java.util.regex.Pattern.compile("\\p{M}+");
    private static final java.util.regex.Pattern NON_ALNUM = java.util.regex.Pattern.compile("[^a-z0-9]+");
    private static final Map<String, String[]> NAME_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static String normalize(String s) {
        String n = MARKS.matcher(Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)).replaceAll("");
        return NON_ALNUM.matcher(n).replaceAll(" ").trim();
    }

    public static String[] tokens(String s) {
        String n = normalize(s);
        return n.isEmpty() ? new String[0] : n.split(" ");
    }

    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) if (i == 0 || s.charAt(i) != s.charAt(i - 1)) sb.append(s.charAt(i));
        return sb.toString();
    }

    /** Distância de Damerau-Levenshtein (trocas de letras adjacentes contam 1). */
    static int distance(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) d[i][0] = i;
        for (int j = 0; j <= m; j++) d[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1))
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
            }
        }
        return d[n][m];
    }

    static double tokenScore(String q, String t) {
        if (q.equals(t)) return 1.0;
        int ql = q.length();
        if (ql >= 2 && t.startsWith(q)) return 0.92;           // palavra escrita parcialmente
        if (ql <= 3) return 0.0;                                // curtas demais para tolerar erro
        int max = ql <= 5 ? 1 : ql <= 8 ? 2 : 3;
        double best = 0;
        if (collapse(q).equals(collapse(t))) best = 0.9;        // letras repetidas
        if (t.length() < ql - max) return best;                 // curta demais: nem tenta
        int d = Math.abs(ql - t.length()) > max ? max + 1 : distance(q, t);
        if (d <= max) best = Math.max(best, 0.88 - 0.10 * (d - 1));
        if (t.length() > ql) {                                  // parcial + erro de digitação
            for (int len = Math.max(1, ql - 1); len <= Math.min(t.length(), ql + 1); len++) {
                int dp = distance(q, t.substring(0, len));
                if (dp <= max) best = Math.max(best, 0.80 - 0.10 * (dp - 1));
            }
        }
        return best;
    }

    /** Pontuação 0..1 de uma consulta contra um nome (0 = não combina). */
    public static double score(String[] q, String[] n) {
        if (q.length == 0 || n.length == 0) return 0;
        boolean[] used = new boolean[n.length];
        double sum = 0, weights = 0;
        for (String qt : q) {
            boolean stop = STOP.contains(qt);
            double w = stop ? 0.3 : 1.0, best = 0;
            int bi = -1;
            for (int j = 0; j < n.length; j++) {
                if (used[j]) continue;
                double s = tokenScore(qt, n[j]);
                if (s > best) { best = s; bi = j; }
            }
            if (bi >= 0) used[bi] = true;
            else if (!stop) return 0;                           // palavra importante sem correspondência
            sum += best * w;
            weights += w;
        }
        int extra = 0;
        for (int j = 0; j < n.length; j++) if (!used[j] && !STOP.contains(n[j])) extra++;
        return Math.max(0, sum / weights - 0.03 * extra);
    }

    private record Scored<T>(T value, double score, int len) {}

    public static <T> List<T> search(String query, Collection<T> items, Function<T, Collection<String>> names,
                                     int limit, double minScore) {
        String[] q = tokens(query);
        if (q.length == 0 || normalize(query).length() < 3) return List.of();
        List<Scored<T>> found = new ArrayList<>();
        for (T item : items) {
            double best = 0;
            int len = 0;
            for (String name : names.apply(item)) {
                if (NAME_CACHE.size() > 40000) NAME_CACHE.clear();
                String[] nt = NAME_CACHE.computeIfAbsent(name, FuzzyMatcher::tokens);
                double s = score(q, nt);
                if (s > best) { best = s; len = name.length(); }
            }
            if (best >= minScore) found.add(new Scored<>(item, best, len));
        }
        if (found.isEmpty()) return List.of();
        found.sort(Comparator.<Scored<T>>comparingDouble(s -> -s.score).thenComparingInt(s -> s.len));
        double cutoff = Math.max(minScore, found.get(0).score - 0.20);
        List<T> out = new ArrayList<>();
        for (Scored<T> s : found) {
            if (s.score < cutoff || out.size() >= limit) break;
            out.add(s.value);
        }
        return out;
    }

    private FuzzyMatcher() {}
}
