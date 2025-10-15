package com.proautokimium.api.domain.models;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SmtpMail {
	private List<String> recipients;
	private String subjects;
	private String body;
	private List<String> cc;
	private List<String> bcc;
	private MultipartFile[] attachments;
	private String imageBase64;
}
