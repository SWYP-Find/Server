package com.swyp.picke.domain.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.battle.repository.BattleTagRepository;
import com.swyp.picke.domain.oauth.jwt.JwtProvider;
import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.domain.tag.enums.TagType;
import com.swyp.picke.domain.tag.repository.TagRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminContentCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private BattleRepository battleRepository;

    @Autowired
    private BattleOptionRepository battleOptionRepository;

    @Autowired
    private BattleTagRepository battleTagRepository;

    @Autowired
    private BattleOptionTagRepository battleOptionTagRepository;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3PresignedUrlService s3PresignedUrlService;

    @Test
    @DisplayName("관리자가 배틀을 생성할 때 현재 매핑된 필드들을 저장한다")
    void createBattle_persistsAllMappedFields() throws Exception {
        User admin = createAdminUser();
        String adminToken = jwtProvider.createAccessToken(admin.getId(), "ADMIN");
        LocalDate targetDate = LocalDate.now();

        Tag category = createTag("battle-category", TagType.CATEGORY);
        Tag philosopher = createTag("battle-philosopher", TagType.PHILOSOPHER);
        Tag value = createTag("battle-value", TagType.VALUE);

        Map<String, Object> payload = Map.of(
                "type", "BATTLE",
                "status", "PENDING",
                "title", "배틀 제목",
                "summary", "배틀 요약",
                "description", "배틀 설명",
                "thumbnailUrl", "images/battles/battle-thumb.png",
                "targetDate", targetDate.toString(),
                "audioDuration", 95,
                "tagIds", List.of(category.getId()),
                "options", List.of(
                        Map.of(
                                "label", "A",
                                "title", "A 선택지",
                                "stance", "A 입장",
                                "representative", "소크라테스",
                                "imageUrl", "images/philosophers/a.png",
                                "displayOrder", 1,
                                "tagIds", List.of(philosopher.getId(), value.getId())
                        ),
                        Map.of(
                                "label", "B",
                                "title", "B 선택지",
                                "stance", "B 입장",
                                "representative", "플라톤",
                                "imageUrl", "images/philosophers/b.png",
                                "displayOrder", 2,
                                "tagIds", List.of(value.getId())
                        )
                )
        );

        MvcResult result = mockMvc.perform(post("/api/v1/admin/battles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.battleId").exists())
                .andExpect(jsonPath("$.data.thumbnailUrl")
                        .value("http://localhost:8080/api/v1/resources/images/BATTLE/battle-thumb.png"))
                .andReturn();

        Long battleId = extractId(result, "battleId");
        Battle savedBattle = battleRepository.findById(battleId).orElseThrow();
        List<BattleOption> options = battleOptionRepository.findByBattle(savedBattle);

        assertThat(savedBattle.getTitle()).isEqualTo("배틀 제목");
        assertThat(savedBattle.getSummary()).isEqualTo("배틀 요약");
        assertThat(savedBattle.getDescription()).isEqualTo("배틀 설명");
        assertThat(savedBattle.getThumbnailUrl()).isEqualTo("images/battles/battle-thumb.png");
        assertThat(savedBattle.getAudioDuration()).isEqualTo(95);
        assertThat(savedBattle.getTargetDate()).isEqualTo(targetDate);

        assertThat(options).hasSize(2);
        BattleOption optionA = options.stream().filter(option -> option.getLabel().name().equals("A")).findFirst().orElseThrow();
        BattleOption optionB = options.stream().filter(option -> option.getLabel().name().equals("B")).findFirst().orElseThrow();

        assertThat(optionA.getTitle()).isEqualTo("A 선택지");
        assertThat(optionA.getRepresentative()).isEqualTo("소크라테스");
        assertThat(optionA.getDisplayOrder()).isNull();
        assertThat(optionB.getTitle()).isEqualTo("B 선택지");
        assertThat(optionB.getRepresentative()).isEqualTo("플라톤");
        assertThat(optionB.getDisplayOrder()).isNull();

        assertThat(battleTagRepository.findByBattle(savedBattle)).hasSize(1);
        assertThat(battleOptionTagRepository.findByBattleOption(optionA)).hasSize(2);
        assertThat(battleOptionTagRepository.findByBattleOption(optionB)).hasSize(1);
    }

    @Test
    @DisplayName("관리자가 퀴즈를 생성할 때 필드가 저장된다")
    void createQuiz_persistsAllMappedFields() throws Exception {
        User admin = createAdminUser();
        String adminToken = jwtProvider.createAccessToken(admin.getId(), "ADMIN");
        LocalDate targetDate = LocalDate.now().plusDays(1);

        Map<String, Object> payload = Map.of(
                "title", "퀴즈 제목",
                "targetDate", targetDate.toString(),
                "status", "PENDING",
                "options", List.of(
                        Map.of(
                                "label", "A",
                                "text", "정답 보기",
                                "detailText", "정답 해설",
                                "isCorrect", true,
                                "displayOrder", 1
                        ),
                        Map.of(
                                "label", "B",
                                "text", "오답 보기",
                                "detailText", "오답 해설",
                                "isCorrect", false,
                                "displayOrder", 2
                        )
                )
        );

        mockMvc.perform(post("/api/v1/admin/quizzes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quizId").exists())
                .andExpect(jsonPath("$.data.targetDate").value(targetDate.toString()))
                .andExpect(jsonPath("$.data.options[0].displayOrder").value(1))
                .andExpect(jsonPath("$.data.options[1].displayOrder").value(2));
    }

    @Test
    @DisplayName("관리자가 투표를 생성할 때 필드가 저장된다")
    void createPoll_persistsAllMappedFields() throws Exception {
        User admin = createAdminUser();
        String adminToken = jwtProvider.createAccessToken(admin.getId(), "ADMIN");
        LocalDate targetDate = LocalDate.now().plusDays(2);

        Map<String, Object> payload = Map.of(
                "titlePrefix", "당신은",
                "titleSuffix", "어느 쪽인가요?",
                "targetDate", targetDate.toString(),
                "status", "PENDING",
                "options", List.of(
                        Map.of(
                                "label", "A",
                                "title", "선택지 A",
                                "displayOrder", 1
                        ),
                        Map.of(
                                "label", "B",
                                "title", "선택지 B",
                                "displayOrder", 2
                        )
                )
        );

        mockMvc.perform(post("/api/v1/admin/polls")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pollId").exists())
                .andExpect(jsonPath("$.data.targetDate").value(targetDate.toString()))
                .andExpect(jsonPath("$.data.options[0].displayOrder").value(1))
                .andExpect(jsonPath("$.data.options[1].displayOrder").value(2));
    }

    @Test
    @DisplayName("리소스 이미지 URL이 사전서명된 URL로 리다이렉트된다")
    void resourceImage_redirects_to_presigned_url() throws Exception {
        String expectedPresignedUrl = "https://signed.example.com/images/battles/test.png?sig=abc";
        when(s3PresignedUrlService.generatePresignedUrl("images/battles/test.png"))
                .thenReturn(expectedPresignedUrl);

        mockMvc.perform(get("/api/v1/resources/images/BATTLE/test.png"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", expectedPresignedUrl));
    }

    @Test
    @DisplayName("대기 중인 로컬 이미지는 게시 시 S3로 옮겨진다")
    void pending_local_images_are_promoted_to_s3_on_publish() throws Exception {
        User admin = createAdminUser();
        String adminToken = jwtProvider.createAccessToken(admin.getId(), "ADMIN");

        String localThumbKey = uploadLocalDraftKey(adminToken, "draft-thumb.png", "draft-thumb");
        String localAKey = uploadLocalDraftKey(adminToken, "draft-a.png", "draft-a");
        String localBKey = uploadLocalDraftKey(adminToken, "draft-b.png", "draft-b");

        Map<String, Object> createPayload = Map.of(
                "type", "BATTLE",
                "status", "PENDING",
                "title", "로컬 임시저장 테스트",
                "summary", "요약",
                "description", "설명",
                "thumbnailUrl", localThumbKey,
                "targetDate", LocalDate.now().toString(),
                "audioDuration", 30,
                "tagIds", List.of(),
                "options", List.of(
                        Map.of(
                                "label", "A",
                                "title", "옵션 A",
                                "stance", "입장 A",
                                "representative", "철학자 A",
                                "imageUrl", localAKey,
                                "displayOrder", 1,
                                "tagIds", List.of()
                        ),
                        Map.of(
                                "label", "B",
                                "title", "옵션 B",
                                "stance", "입장 B",
                                "representative", "철학자 B",
                                "imageUrl", localBKey,
                                "displayOrder", 2,
                                "tagIds", List.of()
                        )
                )
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/battles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.battleId").exists())
                .andReturn();

        Long battleId = extractId(createResult, "battleId");
        Battle pendingBattle = battleRepository.findById(battleId).orElseThrow();
        assertThat(pendingBattle.getThumbnailUrl()).startsWith("local/drafts/");

        Map<String, Object> publishPayload = new LinkedHashMap<>();
        publishPayload.put("status", "PUBLISHED");
        publishPayload.put("title", pendingBattle.getTitle());
        publishPayload.put("summary", pendingBattle.getSummary());
        publishPayload.put("description", pendingBattle.getDescription());
        publishPayload.put("thumbnailUrl", pendingBattle.getThumbnailUrl());
        publishPayload.put("targetDate", LocalDate.now().toString());
        publishPayload.put("tagIds", List.of());
        publishPayload.put("options", List.of(
                Map.of(
                        "label", "A",
                        "title", "옵션 A",
                        "stance", "입장 A",
                        "representative", "철학자 A",
                        "imageUrl", localAKey,
                        "displayOrder", 1,
                        "tagIds", List.of()
                ),
                Map.of(
                        "label", "B",
                        "title", "옵션 B",
                        "stance", "입장 B",
                        "representative", "철학자 B",
                        "imageUrl", localBKey,
                        "displayOrder", 2,
                        "tagIds", List.of()
                )
        ));

        mockMvc.perform(patch("/api/v1/admin/battles/{battleId}", battleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishPayload)))
                .andExpect(status().isOk());

        Battle publishedBattle = battleRepository.findById(battleId).orElseThrow();
        assertThat(publishedBattle.getThumbnailUrl()).startsWith("images/battles/");
        List<BattleOption> publishedOptions = battleOptionRepository.findByBattle(publishedBattle);
        assertThat(publishedOptions).allMatch(option -> option.getImageUrl().startsWith("images/philosophers/"));

        verify(s3Client, atLeastOnce()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    private String uploadLocalDraftKey(String adminToken, String fileName, String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                MediaType.IMAGE_PNG_VALUE,
                content.getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/files/upload/local")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.s3Key").exists())
                .andReturn();

        return objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data")
                .path("s3Key")
                .asText();
    }

    private Long extractId(MvcResult result, String idField) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path(idField).asLong();
    }

    private User createAdminUser() {
        return userRepository.save(
                User.builder()
                        .userTag("adm-" + UUID.randomUUID().toString().substring(0, 8))
                        .nickname("admin")
                        .role(UserRole.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build()
        );
    }

    private Tag createTag(String prefix, TagType type) {
        String normalizedPrefix = prefix.length() > 10 ? prefix.substring(0, 10) : prefix;
        return tagRepository.save(
                Tag.builder()
                        .name(normalizedPrefix + "-" + UUID.randomUUID().toString().substring(0, 8))
                        .type(type)
                        .build()
        );
    }
}
