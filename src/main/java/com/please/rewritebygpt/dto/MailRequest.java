package com.please.rewritebygpt.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MailRequest {
    private String content;
    private String recipient;
    private String tone;
}
