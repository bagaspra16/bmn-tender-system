package com.bnm.tender.ai;

import com.bnm.tender.model.Offer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Agentic merchant: asks Groq for a winning offer package given the buyer's
 * request and any existing competing offers, then returns parsed proposals.
 *
 * Model: openai/gpt-oss-120b (strong reasoning, JSON-mode capable on Groq).
 * Returns a List of {productName, price, quantity, rating, comment} bundles.
 */
public final class GroqAgent {
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    // Llama 3.3 70B Versatile: Groq's strongest model with rock-solid JSON-mode support.
    private static final String MODEL    = "llama-3.3-70b-versatile";

    private static final String API_KEY;

    static {
        String key = System.getenv("GROQ_API_KEY");
        if (key == null || key.isBlank()) {
            try {
                io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.load();
                key = dotenv.get("GROQ_API_KEY");
            } catch (Exception e) {
                System.err.println("Warning: .env file not found or could not be loaded. Ensure GROQ_API_KEY is set.");
            }
        }
        if (key == null || key.isBlank()) {
            System.err.println("CRITICAL: GROQ_API_KEY is missing! Set it in your .env file or environment variables.");
        }
        API_KEY = key != null ? key : "";
    }

    private GroqAgent() {}

    public static class Proposal {
        public final String productName;
        public final String description;
        public final double price;
        public final int    quantity;
        public final double rating;
        public final String comment;

        public Proposal(String productName, String description, double price, int quantity, double rating, String comment) {
            this.productName = productName;
            this.description = description;
            this.price = price;
            this.quantity = quantity;
            this.rating = rating;
            this.comment = comment;
        }
    }

    /**
     * Generate a winning package for `merchantName` against `buyerQuery`.
     * `competing` may be empty (no rivals yet) or contain offers from other merchants.
     */
    public static List<Proposal> generatePackage(String merchantName,
                                                 String buyerQuery,
                                                 String preferences,
                                                 List<Offer> competing) throws Exception {
        String userPrompt = buildUserPrompt(merchantName, buyerQuery, preferences, competing);
        String body = buildRequestBody(userPrompt);

        String json = callGroq(body);
        String content = extractMessageContent(json);
        return parseProposals(content);
    }

    // --------------------------------------------------------------------- HTTP

    private static String callGroq(String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(ENDPOINT).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream in = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        if (status < 200 || status >= 300) {
            throw new RuntimeException("Groq API error " + status + ": " + sb);
        }
        return sb.toString();
    }

    // --------------------------------------------------------------------- Prompt

    private static String buildUserPrompt(String merchantName, String buyerQuery,
                                          String preferences, List<Offer> competing) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the procurement agent for the merchant called \"")
          .append(merchantName).append("\".\n");
        sb.append("A buyer just opened a tender request:\n");
        sb.append("- Buyer query: ").append(buyerQuery == null ? "" : buyerQuery).append("\n");
        if (preferences != null && !preferences.isBlank()) {
            sb.append("- Preferences: ").append(preferences).append("\n");
        }

        if (competing != null && !competing.isEmpty()) {
            sb.append("\nCompeting offers ALREADY submitted by OTHER merchants for this exact request:\n");
            for (Offer o : competing) {
                sb.append("  * Merchant=").append(o.getSeller().getName())
                  .append(" | product=").append(o.getProduct().getName())
                  .append(" | unit_price_idr=").append((long) o.getPrice())
                  .append(" | qty=").append(o.getQuantity())
                  .append(" | rating=").append(o.getRating())
                  .append("\n");
            }
            sb.append("\nYour goal: WIN this tender. The buyer will choose by ranking offers ")
              .append("with score = (rating / unit_price) * 10000. Higher is better. ")
              .append("So beat them by giving lower price and/or higher rating, ")
              .append("AND make the package more attractive (more variety, value bundles, free add-ons).\n");
        } else {
            sb.append("\nNo competing offers yet — be the FIRST and most attractive bid.\n");
        }

        sb.append("\nReturn JSON with this EXACT shape:\n");
        sb.append("{\n");
        sb.append("  \"items\": [\n");
        sb.append("    {\n");
        sb.append("      \"product_name\": \"short product name (max 40 chars)\",\n");
        sb.append("      \"description\":  \"why the buyer will love it (max 80 chars)\",\n");
        sb.append("      \"price\":        unit_price_in_IDR_as_integer,\n");
        sb.append("      \"quantity\":     integer_between_1_and_10,\n");
        sb.append("      \"rating\":       number_between_4.5_and_5.0,\n");
        sb.append("      \"comment\":      \"one-line value-add hook (max 60 chars)\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("Rules:\n");
        sb.append("- Produce 2 to 4 items that together form a coherent package matching the buyer's query.\n");
        sb.append("- Prices in IDR, realistic for Jakarta (Rp 5.000 - Rp 150.000 per unit).\n");
        sb.append("- If competitors exist, undercut their best score (rating/price) by at least 10%.\n");
        sb.append("- Make the comment irresistible: bonuses, freshness, speed, or freebies.\n");
        sb.append("- Output ONLY the JSON object. No prose.\n");
        return sb.toString();
    }

    private static String buildRequestBody(String userPrompt) {
        String sys = "You are a sharp B2C tender-winning agent. Always reply with strict JSON.";
        return "{"
            + "\"model\":\"" + MODEL + "\","
            + "\"temperature\":0.7,"
            + "\"max_tokens\":1024,"
            + "\"response_format\":{\"type\":\"json_object\"},"
            + "\"messages\":["
            + "  {\"role\":\"system\",\"content\":" + jsonStr(sys) + "},"
            + "  {\"role\":\"user\",\"content\":"   + jsonStr(userPrompt) + "}"
            + "]}";
    }

    // --------------------------------------------------------------------- Parsing

    private static String extractMessageContent(String fullJson) {
        // Pull "content":"..." from the first choices[0].message.content occurrence.
        int idx = fullJson.indexOf("\"content\"");
        if (idx < 0) throw new RuntimeException("Groq response has no content: " + fullJson);
        int colon = fullJson.indexOf(':', idx);
        int q1 = fullJson.indexOf('"', colon + 1);
        if (q1 < 0) throw new RuntimeException("Bad content start in: " + fullJson);
        StringBuilder out = new StringBuilder();
        for (int i = q1 + 1; i < fullJson.length(); i++) {
            char c = fullJson.charAt(i);
            if (c == '\\' && i + 1 < fullJson.length()) {
                char n = fullJson.charAt(++i);
                switch (n) {
                    case '"':  out.append('"');  break;
                    case '\\': out.append('\\'); break;
                    case '/':  out.append('/');  break;
                    case 'n':  out.append('\n'); break;
                    case 't':  out.append('\t'); break;
                    case 'r':  out.append('\r'); break;
                    case 'b':  out.append('\b'); break;
                    case 'f':  out.append('\f'); break;
                    case 'u':
                        if (i + 4 < fullJson.length()) {
                            out.append((char) Integer.parseInt(fullJson.substring(i + 1, i + 5), 16));
                            i += 4;
                        }
                        break;
                    default:   out.append(n);
                }
            } else if (c == '"') {
                return out.toString();
            } else {
                out.append(c);
            }
        }
        throw new RuntimeException("Unterminated content string");
    }

    private static List<Proposal> parseProposals(String json) {
        List<Proposal> out = new ArrayList<>();
        int arrStart = json.indexOf("\"items\"");
        if (arrStart < 0) arrStart = 0;
        int lb = json.indexOf('[', arrStart);
        int rb = lastMatchingBracket(json, lb);
        if (lb < 0 || rb < 0) throw new RuntimeException("No items[] in agent reply: " + json);

        int i = lb + 1;
        while (i < rb) {
            int objStart = json.indexOf('{', i);
            if (objStart < 0 || objStart > rb) break;
            int objEnd = matchingBrace(json, objStart);
            if (objEnd < 0 || objEnd > rb) break;
            String obj = json.substring(objStart, objEnd + 1);

            String productName = strField(obj, "product_name");
            String description = strField(obj, "description");
            String comment     = strField(obj, "comment");
            double price       = numField(obj, "price");
            int    qty         = (int) numField(obj, "quantity");
            double rating      = numField(obj, "rating");

            if (productName == null || productName.isBlank()) productName = "Special Item";
            if (description == null) description = productName;
            if (comment == null) comment = "";
            if (qty <= 0) qty = 1;
            if (rating <= 0) rating = 4.8;
            if (rating > 5.0) rating = 5.0;
            if (price <= 0) price = 10000;

            out.add(new Proposal(productName, description, price, qty, rating, comment));
            i = objEnd + 1;
        }
        if (out.isEmpty()) throw new RuntimeException("Agent returned 0 items: " + json);
        return out;
    }

    private static String strField(String obj, String key) {
        int k = obj.indexOf("\"" + key + "\"");
        if (k < 0) return null;
        int colon = obj.indexOf(':', k);
        int q1 = obj.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = q1 + 1; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (c == '\\' && i + 1 < obj.length()) { sb.append(obj.charAt(++i)); continue; }
            if (c == '"') return sb.toString();
            sb.append(c);
        }
        return null;
    }

    private static double numField(String obj, String key) {
        int k = obj.indexOf("\"" + key + "\"");
        if (k < 0) return 0;
        int colon = obj.indexOf(':', k);
        int i = colon + 1;
        while (i < obj.length() && Character.isWhitespace(obj.charAt(i))) i++;
        int start = i;
        while (i < obj.length()) {
            char c = obj.charAt(i);
            if (Character.isDigit(c) || c == '.' || c == '-') i++; else break;
        }
        if (i == start) return 0;
        try { return Double.parseDouble(obj.substring(start, i)); }
        catch (NumberFormatException e) { return 0; }
    }

    private static int matchingBrace(String s, int open) {
        int depth = 0;
        boolean inStr = false;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inStr = false;
            } else {
                if (c == '"') inStr = true;
                else if (c == '{') depth++;
                else if (c == '}') { depth--; if (depth == 0) return i; }
            }
        }
        return -1;
    }

    private static int lastMatchingBracket(String s, int open) {
        int depth = 0;
        boolean inStr = false;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inStr = false;
            } else {
                if (c == '"') inStr = true;
                else if (c == '[') depth++;
                else if (c == ']') { depth--; if (depth == 0) return i; }
            }
        }
        return -1;
    }

    private static String jsonStr(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
