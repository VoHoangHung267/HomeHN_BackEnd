package com.homehn.backend.service.impl;

import com.homehn.backend.config.GeminiProperties;
import com.homehn.backend.dto.request.AiSearchRequest;
import com.homehn.backend.dto.request.ChatAssistantRequest;
import com.homehn.backend.dto.request.ExtractRoomFormRequest;
import com.homehn.backend.dto.request.GenerateRoomDescriptionRequest;
import com.homehn.backend.dto.response.AiSearchResponse;
import com.homehn.backend.dto.response.ChatAssistantResponse;
import com.homehn.backend.dto.response.ExtractRoomFormResponse;
import com.homehn.backend.dto.response.GenerateRoomDescriptionResponse;
import com.homehn.backend.dto.response.RoomResponse;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class GeminiRoomCopyService {

    private static final Logger log = LoggerFactory.getLogger(GeminiRoomCopyService.class);

    private static final List<String> HANOI_WARDS = List.of(
            "Hoàn Kiếm", "Cửa Nam", "Ba Đình", "Ngọc Hà", "Giảng Võ",
            "Hai Bà Trưng", "Vĩnh Tuy", "Bạch Mai", "Đống Đa", "Kim Liên",
            "Văn Miếu - Quốc Tử Giám", "Láng", "Ô Chợ Dừa", "Hồng Hà", "Lĩnh Nam",
            "Hoàng Mai", "Vĩnh Hưng", "Tương Mai", "Định Công", "Hoàng Liệt",
            "Yên Sở", "Thanh Xuân", "Khương Đình", "Phương Liệt", "Cầu Giấy",
            "Nghĩa Đô", "Yên Hòa", "Tây Hồ", "Phú Thượng", "Tây Tựu",
            "Phú Diễn", "Xuân Đỉnh", "Đông Ngạc", "Thượng Cát", "Từ Liêm",
            "Xuân Phương", "Tây Mỗ", "Đại Mỗ", "Long Biên", "Bồ Đề",
            "Việt Hưng", "Phúc Lợi", "Hà Đông", "Dương Nội", "Yên Nghĩa",
            "Phú Lương", "Kiến Hưng", "Thanh Liệt", "Chương Mỹ", "Sơn Tây",
            "Tùng Thiện", "Thanh Trì", "Đại Thanh", "Nam Phù", "Ngọc Hồi",
            "Thượng Phúc", "Thường Tín", "Chương Dương", "Hồng Vân", "Phú Xuyên",
            "Phượng Dực", "Chuyên Mỹ", "Đại Xuyên", "Thanh Oai", "Bình Minh",
            "Tam Hưng", "Dân Hòa", "Vân Đình", "Ứng Thiên", "Hòa Xá",
            "Ứng Hòa", "Mỹ Đức", "Hồng Sơn", "Phúc Sơn", "Hương Sơn",
            "Phú Nghĩa", "Xuân Mai", "Trần Phú", "Hòa Phú", "Quảng Bị",
            "Minh Châu", "Quảng Oai", "Vật Lại", "Cổ Đô", "Bất Bạt",
            "Suối Hai", "Ba Vì", "Yên Bài", "Đoài Phương", "Phúc Thọ",
            "Phúc Lộc", "Hát Môn", "Thạch Thất", "Hạ Bằng", "Tây Phương",
            "Hòa Lạc", "Yên Xuân", "Quốc Oai", "Hưng Đạo", "Kiều Phú",
            "Phú Cát", "Hoài Đức", "Dương Hòa", "Sơn Đồng", "An Khánh",
            "Đan Phượng", "Ô Diên", "Liên Minh", "Gia Lâm", "Thuận An",
            "Bát Tràng", "Phù Đổng", "Thư Lâm", "Đông Anh", "Phúc Thịnh",
            "Thiên Lộc", "Vĩnh Thanh", "Mê Linh", "Yên Lãng", "Tiến Thắng",
            "Quang Minh", "Sóc Sơn", "Đa Phúc", "Nội Bài", "Trung Giã",
            "Kim Anh"
    );

    private static final Set<String> ALLOWED_AMENITIES = Set.of(
            "WiFi",
            "Điều hòa",
            "Máy giặt",
            "Tủ lạnh",
            "Bếp từ",
            "Nước nóng",
            "Ban công",
            "Thang máy",
            "Giữ xe máy",
            "Giữ ô tô",
            "Camera an ninh",
            "Bảo vệ 24/7"
    );

    private final GeminiProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public GenerateRoomDescriptionResponse generateDescription(GenerateRoomDescriptionRequest request) {
        ensureEnabled();
        try {
            String text = requestJson(buildGeneratePrompt(request), "generate-description");
            Map<String, Object> output = parseJsonObject(text, "generate-description");
            return new GenerateRoomDescriptionResponse(
                    stringValue(output.get("suggestedTitle")),
                    stringValue(output.get("suggestedDescription"))
            );
        } catch (IOException e) {
            throw new AppException("Không đọc được phản hồi từ Gemini.", 502);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("Yêu cầu tới Gemini bị gián đoạn.", 502);
        } catch (AppException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Gemini generate-description runtime error", e);
            throw new AppException("Không phân tích được phản hồi từ Gemini.", 502);
        }
    }

    public ExtractRoomFormResponse extractRoomForm(ExtractRoomFormRequest request) {
        ensureEnabled();
        String rawDescription = request.getRawDescription() == null ? "" : request.getRawDescription().trim();
        if (rawDescription.isBlank()) {
            throw new AppException("Vui lòng nhập mô tả phòng trước khi dùng AI.");
        }

        try {
            String text = requestJson(buildExtractPrompt(rawDescription), "extract-room-form");
            Map<String, Object> output = parseJsonObject(text, "extract-room-form");
            return new ExtractRoomFormResponse(
                    nullableString(output.get("title")),
                    nullableString(output.get("description")),
                    nullableDecimal(output.get("price")),
                    nullableDecimal(output.get("area")),
                    nullableDecimal(output.get("electricPrice")),
                    nullableDecimal(output.get("waterPrice")),
                    nullableDecimal(output.get("otherFees")),
                    nullableString(output.get("address")),
                    nullableString(output.get("ward")),
                    nullableString(output.get("city")),
                    parseRoomType(output.get("roomType")),
                    nullableBoolean(output.get("isFurnished")),
                    nullableInteger(output.get("maxPeople")),
                    parseGenderRequirement(output.get("genderRequirement")),
                    parseAmenities(output.get("amenities")),
                    nullableString(output.get("note"))
            );
        } catch (IOException e) {
            throw new AppException("Không đọc được phản hồi từ Gemini.", 502);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("Yêu cầu tới Gemini bị gián đoạn.", 502);
        } catch (AppException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Gemini extract-room-form runtime error", e);
            throw new AppException("Không phân tích được thông tin phòng từ mô tả.", 502);
        }
    }

    public AiSearchResponse parseSearch(AiSearchRequest request) {
        ensureEnabled();
        String query = request.getQuery() == null ? "" : request.getQuery().trim();
        if (query.isBlank()) {
            throw new AppException("Vui lòng nhập nhu cầu tìm phòng.");
        }

        try {
            String text = requestJson(buildSearchPrompt(query), "parse-search");
            Map<String, Object> output = parseJsonObject(text, "parse-search");
            return normalizeAiSearchResponse(query, output);
        } catch (IOException e) {
            throw new AppException("Không đọc được phản hồi từ Gemini.", 502);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("Yêu cầu tới Gemini bị gián đoạn.", 502);
        } catch (AppException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Gemini parse-search runtime error", e);
            throw new AppException("Không phân tích được nhu cầu tìm phòng.", 502);
        }
    }

    public ChatAssistantResponse answerRoomQuestion(RoomResponse room, ChatAssistantRequest request) {
        ensureEnabled();
        String question = request.getQuestion() == null ? "" : request.getQuestion().trim();
        if (question.isBlank()) {
            throw new AppException("Vui lòng nhập câu hỏi cho trợ lý AI.");
        }

        try {
            String text = requestJson(buildChatPrompt(room, question), "chat-room");
            Map<String, Object> output = parseJsonObject(text, "chat-room");
            return new ChatAssistantResponse(
                    nullableString(output.get("answer")),
                    nullableString(output.get("note")),
                    nullableString(output.get("actionLabel")),
                    normalizeActionUrl(output.get("actionUrl"))
            );
        } catch (IOException e) {
            throw new AppException("Không đọc được phản hồi từ Gemini.", 502);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("Yêu cầu tới Gemini bị gián đoạn.", 502);
        } catch (AppException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Gemini room-chat runtime error", e);
            throw new AppException("Không thể tạo tư vấn AI cho phòng này.", 502);
        }
    }

    public ChatAssistantResponse answerGeneralQuestion(ChatAssistantRequest request) {
        ensureEnabled();
        String question = request.getQuestion() == null ? "" : request.getQuestion().trim();
        if (question.isBlank()) {
            throw new AppException("Vui lòng nhập câu hỏi cho trợ lý AI.");
        }

        try {
            String text = requestJson(buildGeneralAssistantPrompt(question), "chat-general");
            Map<String, Object> output = parseJsonObject(text, "chat-general");
            return new ChatAssistantResponse(
                    nullableString(output.get("answer")),
                    nullableString(output.get("note")),
                    nullableString(output.get("actionLabel")),
                    normalizeActionUrl(output.get("actionUrl"))
            );
        } catch (IOException e) {
            throw new AppException("Không đọc được phản hồi từ Gemini.", 502);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("Yêu cầu tới Gemini bị gián đoạn.", 502);
        } catch (AppException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Gemini general-chat runtime error", e);
            throw new AppException("Không thể tạo phản hồi từ trợ lý AI.", 502);
        }
    }

    private void ensureEnabled() {
        if (!properties.isEnabled() || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AppException("Gemini chưa được cấu hình. Vui lòng thêm GEMINI_API_KEY.");
        }
    }

    private String requestJson(String prompt, String useCase) throws IOException, InterruptedException {
        String endpoint = properties.getBaseUrl()
                + "/models/" + urlEncode(properties.getModel())
                + ":generateContent?key=" + urlEncode(properties.getApiKey());

        String payload = """
                {
                  "contents": [
                    {
                      "role": "user",
                      "parts": [
                        {
                          "text": "%s"
                        }
                      ]
                    }
                  ],
                  "generationConfig": {
                    "temperature": 0.25,
                    "responseMimeType": "application/json"
                  }
                }
                """.formatted(jsonEscape(prompt));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(40))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() >= 400) {
            log.error("Gemini {} HTTP error status={} body={}", useCase, response.statusCode(), abbreviate(response.body()));
            throw mapGeminiHttpException(response.statusCode(), response.body());
        }

        Map<String, Object> root;
        try {
            root = JsonParserFactory.getJsonParser().parseMap(response.body());
        } catch (RuntimeException ex) {
            log.error("Gemini {} root payload is not valid JSON: {}", useCase, abbreviate(response.body()), ex);
            throw new AppException("Gemini trả về payload không hợp lệ.", 502);
        }

        String text = extractCandidateText(root);
        if (text.isBlank()) {
            String blockedReason = nestedString(root, "promptFeedback", "blockReason");
            if (!blockedReason.isBlank()) {
                log.error("Gemini {} blocked request reason={}", useCase, blockedReason);
                throw new AppException("Gemini từ chối yêu cầu: " + blockedReason, 502);
            }
            log.error("Gemini {} returned empty candidate text body={}", useCase, abbreviate(response.body()));
            throw new AppException("Gemini không trả về nội dung hợp lệ.", 502);
        }

        return unwrapJsonPayload(text);
    }

    private Map<String, Object> parseJsonObject(String text, String useCase) {
        try {
            return JsonParserFactory.getJsonParser().parseMap(text);
        } catch (RuntimeException ex) {
            log.error("Gemini {} returned invalid JSON payload: {}", useCase, abbreviate(text), ex);
            throw new AppException("Gemini trả về JSON không hợp lệ.", 502);
        }
    }

    private AppException mapGeminiHttpException(int statusCode, String responseBody) {
        Map<String, Object> payload = tryParseErrorPayload(responseBody);
        String upstreamStatus = nestedString(payload, "error", "status");
        String upstreamMessage = nestedString(payload, "error", "message");
        String normalizedUpstream = (upstreamStatus + " " + upstreamMessage).toLowerCase();

        if (statusCode == 429 || normalizedUpstream.contains("resource_exhausted") || normalizedUpstream.contains("quota")) {
            return new AppException("AI đang hết quota tạm thời. Vui lòng thử lại sau ít phút.", 503);
        }

        if (statusCode == 503 || normalizedUpstream.contains("unavailable") || normalizedUpstream.contains("high demand")) {
            return new AppException("AI đang bận do lượng yêu cầu cao. Vui lòng thử lại sau ít phút.", 503);
        }

        if (statusCode == 403 || normalizedUpstream.contains("permission_denied") || normalizedUpstream.contains("api key")) {
            return new AppException("AI trên server đang cấu hình chưa hợp lệ. Vui lòng liên hệ quản trị viên để kiểm tra API key.", 502);
        }

        if (!upstreamMessage.isBlank()) {
            return new AppException("AI tạm thời chưa thể xử lý yêu cầu: " + upstreamMessage, 502);
        }

        return new AppException("AI tạm thời chưa phản hồi hợp lệ. Vui lòng thử lại sau.", 502);
    }

    private Map<String, Object> tryParseErrorPayload(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return Map.of();
        }

        try {
            return JsonParserFactory.getJsonParser().parseMap(responseBody);
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000) + "...";
    }

    private String buildGeneratePrompt(GenerateRoomDescriptionRequest request) {
        StringJoiner prompt = new StringJoiner("\n");
        prompt.add("Bạn là trợ lý viết nội dung cho website cho thuê phòng trọ tại Việt Nam.");
        prompt.add("Hãy tạo 1 tiêu đề ngắn gọn, rõ ràng và 1 mô tả tin đăng tự nhiên, dễ đọc, trung thực.");
        prompt.add("Không phóng đại, không thêm thông tin không có trong dữ liệu.");
        prompt.add("Giọng văn thân thiện, rõ ràng, ưu tiên nêu giá, vị trí, diện tích, tiện ích, đối tượng phù hợp.");
        prompt.add("Chỉ trả về JSON hợp lệ với đúng 2 key: suggestedTitle, suggestedDescription.");
        prompt.add("Mô tả nên dài khoảng 80-160 từ.");
        prompt.add("");
        prompt.add("Dữ liệu đầu vào:");
        prompt.add("- Tiêu đề nháp: " + safe(request.getTitle()));
        prompt.add("- Mô tả hiện tại: " + safe(request.getCurrentDescription()));
        prompt.add("- Loại phòng: " + enumText(request.getRoomType()));
        prompt.add("- Giá thuê: " + moneyText(request.getPrice()));
        prompt.add("- Diện tích: " + decimalText(request.getArea(), "m2"));
        prompt.add("- Giá điện: " + moneyText(request.getElectricPrice()));
        prompt.add("- Giá nước: " + moneyText(request.getWaterPrice()));
        prompt.add("- Phí khác: " + moneyText(request.getOtherFees()));
        prompt.add("- Địa chỉ: " + joinAddress(request.getAddress(), request.getWard(), request.getCity()));
        prompt.add("- Có nội thất: " + booleanText(request.getIsFurnished()));
        prompt.add("- Số người tối đa: " + numberText(request.getMaxPeople()));
        prompt.add("- Đối tượng phù hợp: " + enumText(request.getGenderRequirement()));
        prompt.add("- Tiện ích: " + listText(request.getAmenities()));
        return prompt.toString();
    }

    private String buildExtractPrompt(String rawDescription) {
        StringJoiner prompt = new StringJoiner("\n");
        prompt.add("Bạn là trợ lý điền form đăng phòng trọ tại Việt Nam.");
        prompt.add("Hãy đọc mô tả tự do của người dùng và trích xuất các trường để điền form.");
        prompt.add("Chỉ dùng thông tin có trong mô tả hoặc suy ra rất rõ ràng. Nếu không chắc thì trả về null.");
        prompt.add("Amenities chỉ được chọn từ danh sách sau: WiFi, Điều hòa, Máy giặt, Tủ lạnh, Bếp từ, Nước nóng, Ban công, Thang máy, Giữ xe máy, Giữ ô tô, Camera an ninh, Bảo vệ 24/7.");
        prompt.add("roomType chỉ được là: PHONG_TRO, CHUNG_CU_MINI, STUDIO, NGAN_PHONG, NHA_NGUYEN_CAN.");
        prompt.add("genderRequirement chỉ được là: ALL, MALE, FEMALE.");
        prompt.add("Nếu mô tả nói rõ ở Hà Nội thì có thể điền city = \"Hà Nội\".");
        prompt.add("description là bản viết lại gọn gàng, phù hợp để đăng tin.");
        prompt.add("title là tiêu đề tin đăng ngắn gọn, rõ ràng.");
        prompt.add("note là một câu ngắn nói những trường còn thiếu hoặc chưa chắc, hoặc null nếu đã khá đầy đủ.");
        prompt.add("Chỉ trả về JSON hợp lệ với các key đúng thứ tự sau:");
        prompt.add("title, description, price, area, electricPrice, waterPrice, otherFees, address, ward, city, roomType, isFurnished, maxPeople, genderRequirement, amenities, note");
        prompt.add("");
        prompt.add("Mô tả người dùng:");
        prompt.add(rawDescription);
        return prompt.toString();
    }

    private String buildSearchPrompt(String query) {
        StringJoiner prompt = new StringJoiner("\n");
        prompt.add("Bạn là trợ lý tìm kiếm phòng trọ tại Việt Nam.");
        prompt.add("Hãy đọc nhu cầu tự nhiên của người dùng và trích xuất bộ lọc tìm kiếm.");
        prompt.add("Chỉ trả về JSON hợp lệ với các key: keyword, ward, minPrice, maxPrice, minArea, maxArea, roomType, isFurnished, genderRequirement, sortBy, note.");
        prompt.add("roomType chỉ được là: PHONG_TRO, CHUNG_CU_MINI, STUDIO, NGAN_PHONG, NHA_NGUYEN_CAN hoặc null.");
        prompt.add("genderRequirement chỉ được là: ALL, MALE, FEMALE hoặc null.");
        prompt.add("sortBy chỉ được là: createdAt, price_asc, price_desc, viewCount hoặc null.");
        prompt.add("Nếu người dùng chỉ nói 'gần', 'yên tĩnh', 'ở ngay', 'an ninh' thì có thể đưa phần còn lại vào keyword để hệ thống tìm tương đối.");
        prompt.add("Nếu người dùng nhắc đến trường đại học, học viện, cao đẳng, bến xe, bệnh viện hoặc landmark, hãy giữ tên địa điểm đó trong keyword.");
        prompt.add("Nếu người dùng nhắc tới phường/xã cụ thể thì điền ward theo đúng tên đó.");
        prompt.add("Không dùng trường quận/huyện. Nếu người dùng nói tên quận cũ hoặc thị xã cũ, hãy ưu tiên giữ trong keyword thay vì tạo field riêng.");
        prompt.add("Giá tiền và diện tích phải là số, không có đơn vị.");
        prompt.add("Nếu không chắc một trường thì trả về null.");
        prompt.add("note là một câu ngắn tóm tắt cách bạn hiểu nhu cầu tìm kiếm.");
        prompt.add("");
        prompt.add("Nhu cầu người dùng:");
        prompt.add(query);
        return prompt.toString();
    }

    private String buildChatPrompt(RoomResponse room, String question) {
        StringJoiner prompt = new StringJoiner("\n");
        prompt.add("Bạn là trợ lý tư vấn thuê phòng.");
        prompt.add("Chỉ được trả lời dựa trên dữ liệu phòng được cung cấp. Không bịa thêm thông tin.");
        prompt.add("Nếu thiếu dữ liệu để kết luận, hãy nói rõ là chưa đủ thông tin.");
        prompt.add("Câu trả lời bằng tiếng Việt, ngắn gọn, thực dụng, tối đa 120 từ.");
        prompt.add("Chỉ trả về JSON hợp lệ với 4 key: answer, note, actionLabel, actionUrl.");
        prompt.add("actionLabel và actionUrl chỉ dùng khi có một bước điều hướng rõ ràng. Nếu không cần thì trả về null.");
        prompt.add("actionUrl chỉ được là một trong các route nội bộ sau hoặc null: /chat, /rooms, /map, /appointments, /bookings, /profile.");
        prompt.add("");
        prompt.add("Dữ liệu phòng:");
        prompt.add("- Tiêu đề: " + safe(room.getTitle()));
        prompt.add("- Mô tả: " + safe(room.getDescription()));
        prompt.add("- Giá thuê: " + moneyText(room.getPrice()));
        prompt.add("- Diện tích: " + decimalText(room.getArea(), "m2"));
        prompt.add("- Điện: " + moneyText(room.getElectricPrice()) + " / kWh");
        prompt.add("- Nước: " + moneyText(room.getWaterPrice()) + " / m3");
        prompt.add("- Phí khác: " + moneyText(room.getOtherFees()));
        prompt.add("- Địa chỉ: " + joinAddress(room.getAddress(), room.getWard(), room.getCity()));
        prompt.add("- Loại phòng: " + enumText(room.getRoomType()));
        prompt.add("- Nội thất: " + booleanText(room.getIsFurnished()));
        prompt.add("- Số người tối đa: " + numberText(room.getMaxPeople()));
        prompt.add("- Đối tượng: " + enumText(room.getGenderRequirement()));
        prompt.add("- Tiện ích: " + listText(room.getAmenities()));
        prompt.add("");
        prompt.add("Câu hỏi của người dùng:");
        prompt.add(question);
        return prompt.toString();
    }

    private String buildGeneralAssistantPrompt(String question) {
        StringJoiner prompt = new StringJoiner("\n");
        prompt.add("Bạn là trợ lý AI cho website HomeHN.vn chuyên tìm và đăng phòng trọ.");
        prompt.add("Hãy trả lời ngắn gọn, rõ ràng, thực dụng bằng tiếng Việt.");
        prompt.add("Bạn có thể hỗ trợ:");
        prompt.add("- hướng dẫn tìm phòng, lọc phòng, đặt lịch xem phòng, gửi yêu cầu thuê phòng, nhắn tin chủ nhà");
        prompt.add("- tư vấn cách viết tin đăng tốt hơn cho chủ nhà");
        prompt.add("- giải thích các bước dùng website");
        prompt.add("Không bịa ra dữ liệu cụ thể về một phòng nếu người dùng chưa cung cấp.");
        prompt.add("Nếu câu hỏi cần dữ liệu của một phòng cụ thể, hãy nói người dùng mở trang chi tiết phòng hoặc cuộc trò chuyện liên quan.");
        prompt.add("Chỉ trả về JSON hợp lệ với 4 key: answer, note, actionLabel, actionUrl.");
        prompt.add("Nếu người dùng nên đi đến một khu cụ thể trên website, hãy điền actionLabel và actionUrl.");
        prompt.add("actionUrl chỉ được là một trong các route nội bộ sau hoặc null: /rooms, /map, /chat, /appointments, /bookings, /profile, /auth/login, /auth/register, /landlord.");
        prompt.add("Ví dụ: muốn xem phòng trên bản đồ thì actionUrl=/map; muốn bắt đầu tìm phòng thì actionUrl=/rooms; muốn xem đơn thuê thì actionUrl=/bookings.");
        prompt.add("Nếu không cần điều hướng thì actionLabel = null và actionUrl = null.");
        prompt.add("");
        prompt.add("Câu hỏi người dùng:");
        prompt.add(question);
        return prompt.toString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Không có" : value.trim();
    }

    private String joinAddress(String address, String ward, String city) {
        StringJoiner joiner = new StringJoiner(", ");
        addPart(joiner, address);
        addPart(joiner, ward);
        addPart(joiner, city);
        String result = joiner.toString();
        return result.isBlank() ? "Không có" : result;
    }

    private void addPart(StringJoiner joiner, String value) {
        if (value != null && !value.isBlank()) {
            joiner.add(value.trim());
        }
    }

    private String booleanText(Boolean value) {
        if (value == null) return "Không rõ";
        return value ? "Có" : "Không";
    }

    private String numberText(Integer value) {
        return value == null ? "Không rõ" : String.valueOf(value);
    }

    private String decimalText(BigDecimal value, String suffix) {
        return value == null ? "Không rõ" : value.stripTrailingZeros().toPlainString() + " " + suffix;
    }

    private String moneyText(BigDecimal value) {
        return value == null ? "Không rõ" : value.stripTrailingZeros().toPlainString() + " VNĐ";
    }

    private String listText(List<String> values) {
        return values == null || values.isEmpty() ? "Không có" : String.join(", ", values);
    }

    private String enumText(RoomEntity.RoomType type) {
        if (type == null) return "Không rõ";
        return switch (type) {
            case PHONG_TRO -> "Phòng trọ";
            case CHUNG_CU_MINI -> "Chung cư mini";
            case STUDIO -> "Studio";
            case NGAN_PHONG -> "Ngăn phòng";
            case NHA_NGUYEN_CAN -> "Nhà nguyên căn";
        };
    }

    private String enumText(RoomEntity.GenderRequirement value) {
        if (value == null) return "Không rõ";
        return switch (value) {
            case ALL -> "Tất cả";
            case MALE -> "Nam";
            case FEMALE -> "Nữ";
        };
    }

    private String parseSortBy(Object value) {
        String result = nullableString(value);
        if (result == null) return null;
        return switch (result) {
            case "createdAt", "price_asc", "price_desc", "viewCount" -> result;
            default -> null;
        };
    }

    private AiSearchResponse normalizeAiSearchResponse(String query, Map<String, Object> output) {
        String keyword = nullableString(output.get("keyword"));
        String ward = canonicalizeWard(nullableString(output.get("ward")));

        String matchedWardFromQuery = findWardMention(query);
        if (matchedWardFromQuery != null) {
            ward = matchedWardFromQuery;
        }

        return new AiSearchResponse(
                keyword,
                ward,
                nullableDecimal(output.get("minPrice")),
                nullableDecimal(output.get("maxPrice")),
                nullableDecimal(output.get("minArea")),
                nullableDecimal(output.get("maxArea")),
                parseRoomType(output.get("roomType")),
                nullableBoolean(output.get("isFurnished")),
                parseGenderRequirement(output.get("genderRequirement")),
                parseSortBy(output.get("sortBy")),
                nullableString(output.get("note"))
        );
    }

    private String findWardMention(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String normalizedText = normalizeLocation(text);
        for (String ward : HANOI_WARDS) {
            if (normalizedText.contains(normalizeLocation(ward))) {
                return ward;
            }
        }
        return null;
    }

    private String canonicalizeWard(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = normalizeLocation(value);
        for (String ward : HANOI_WARDS) {
            if (normalizeLocation(ward).equals(normalizedValue)) {
                return ward;
            }
        }
        return null;
    }

    private boolean normalizedContains(String source, String token) {
        if (source == null || token == null || source.isBlank() || token.isBlank()) {
            return false;
        }
        return normalizeLocation(source).contains(normalizeLocation(token));
    }

    private String normalizeLocation(String value) {
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String normalizeActionUrl(Object value) {
        String result = nullableString(value);
        if (result == null) {
            return null;
        }
        return switch (result) {
            case "/rooms", "/map", "/chat", "/appointments", "/bookings",
                    "/profile", "/auth/login", "/auth/register", "/landlord" -> result;
            default -> null;
        };
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String jsonEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    @SuppressWarnings("unchecked")
    private String nestedString(Map<String, Object> root, Object... path) {
        Object current = root;
        for (Object segment : path) {
            if (current instanceof Map<?, ?> map && segment instanceof String key) {
                current = ((Map<String, Object>) map).get(key);
            } else if (current instanceof List<?> list && segment instanceof Integer index) {
                current = index >= 0 && index < list.size() ? list.get(index) : null;
            } else {
                return "";
            }
            if (current == null) {
                return "";
            }
        }
        return stringValue(current);
    }

    @SuppressWarnings("unchecked")
    private String extractCandidateText(Map<String, Object> root) {
        Object candidatesObject = root.get("candidates");
        if (!(candidatesObject instanceof List<?> candidates) || candidates.isEmpty()) {
            return "";
        }

        Object firstCandidate = candidates.get(0);
        if (!(firstCandidate instanceof Map<?, ?> candidateMap)) {
            return "";
        }

        Object contentObject = ((Map<String, Object>) candidateMap).get("content");
        if (!(contentObject instanceof Map<?, ?> contentMap)) {
            return "";
        }

        Object partsObject = ((Map<String, Object>) contentMap).get("parts");
        if (!(partsObject instanceof List<?> parts) || parts.isEmpty()) {
            return "";
        }

        StringJoiner text = new StringJoiner("\n");
        for (Object part : parts) {
            if (part instanceof Map<?, ?> partMap) {
                String partText = stringValue(((Map<String, Object>) partMap).get("text")).trim();
                if (!partText.isBlank()) {
                    text.add(partText);
                }
            }
        }
        return text.toString().trim();
    }

    private String unwrapJsonPayload(String rawText) {
        String cleaned = rawText == null ? "" : rawText.trim();
        if (cleaned.isBlank()) {
            return "";
        }

        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline >= 0) {
                cleaned = cleaned.substring(firstNewline + 1).trim();
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
            }
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullableString(Object value) {
        String result = stringValue(value).trim();
        return result.isBlank() || "null".equalsIgnoreCase(result) ? null : result;
    }

    private BigDecimal nullableDecimal(Object value) {
        String result = nullableString(value);
        if (result == null) return null;
        try {
            return new BigDecimal(result);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer nullableInteger(Object value) {
        String result = nullableString(value);
        if (result == null) return null;
        try {
            return Integer.parseInt(result);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean nullableBoolean(Object value) {
        String result = nullableString(value);
        if (result == null) return null;
        return Boolean.parseBoolean(result);
    }

    private RoomEntity.RoomType parseRoomType(Object value) {
        String result = nullableString(value);
        if (result == null) return null;
        try {
            return RoomEntity.RoomType.valueOf(result);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private RoomEntity.GenderRequirement parseGenderRequirement(Object value) {
        String result = nullableString(value);
        if (result == null) return null;
        try {
            return RoomEntity.GenderRequirement.valueOf(result);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<String> parseAmenities(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (Object item : list) {
            String amenity = String.valueOf(item).trim();
            if (!amenity.isBlank() && ALLOWED_AMENITIES.contains(amenity)) {
                normalized.add(amenity);
            }
        }
        return new ArrayList<>(normalized);
    }
}
