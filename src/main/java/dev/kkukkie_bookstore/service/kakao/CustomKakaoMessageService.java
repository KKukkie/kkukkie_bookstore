package dev.kkukkie_bookstore.service.kakao;

import dev.kkukkie_bookstore.service.admin.AdminAuthService;
import org.json.JSONException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomKakaoMessageService {

    private final AdminAuthService authService;

    private final KakaoAuthService kakaoAuthService;

    private final MessageService messageService;

    public CustomKakaoMessageService(AdminAuthService authService,
                                     KakaoAuthService kakaoAuthService,
                                     MessageService messageService) {
        this.authService = authService;
        this.kakaoAuthService = kakaoAuthService;
        this.messageService = messageService;
    }

    public String sendMyMessage() throws JSONException {
        DefaultKakaoMessageDto myMsg = new DefaultKakaoMessageDto();
        myMsg.setBtnTitle("");
        myMsg.setMobileUrl("");
        myMsg.setObjType("text");
        myMsg.setWebUrl("");

        // AdminAuthService 에 UUID 등록
        String authCode = UUID.randomUUID().toString().substring(0, 8);
        authService.addAuthCode(authCode);
        myMsg.setText(authCode);

        String accessToken = kakaoAuthService.getAuthToken();
        return messageService.sendMessage(accessToken, myMsg)? authCode : "";
    }
}
