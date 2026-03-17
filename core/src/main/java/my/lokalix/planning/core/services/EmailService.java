package my.lokalix.planning.core.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.models.smtp2go.EmailRequest;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class EmailService {

  private WebClient webClient;
  @Resource private AppConfigurationProperties appConfigurationProperties;
  @Resource private SpringTemplateEngine templateEngine;

  @PostConstruct
  private void init() {
    webClient =
        WebClient.builder()
            .baseUrl(appConfigurationProperties.getSmtp2go().getBaseUrl() + "/email/send")
            .build();
  }

  public Mono<Void> sendEmail(String recipientEmail, String subject, String body) {
    // Build the request
    EmailRequest email =
        new EmailRequest(
            List.of(recipientEmail),
            appConfigurationProperties.getSmtp2go().getSender(),
            subject,
            body);

    // Sending the request
    return webClient
        .post()
        .header(
            appConfigurationProperties.getSmtp2go().getApiKeyHeader(),
            appConfigurationProperties.getSmtp2go().getApiKey())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(Mono.just(email), EmailRequest.class)
        .retrieve()
        .bodyToMono(Void.class);
  }

  public Mono<Void> sendEmailToMultipleRecipients(
      List<String> recipientEmails, String subject, String body) {
    return Flux.fromIterable(recipientEmails)
        .flatMap(
            recipientEmail ->
                sendEmail(recipientEmail, subject, body)
                    .onErrorResume(
                        e -> {
                          // Log the error but continue with other emails
                          log.error(
                              "Failed to send email to {}: {}", recipientEmail, e.getMessage());
                          return Mono.empty();
                        }))
        .then();
  }

  @Async
  public void sendAccountCreationEmail(String email, String token) {
    String htmlBody = buildUserCreationEmailTemplate(token);
    sendEmail(email, "Account created", htmlBody).subscribe();
  }

  private String buildUserCreationEmailTemplate(String token) {
    Context context = new Context();
    context.setVariable(
        "passwordSetupLink",
        appConfigurationProperties.getAppBaseUrl() + "/set-first-password?token=" + token);
    return templateEngine.process("set-password-email-template", context);
  }

  @Async
  public void sendPasswordResetEmail(String email, String token) {
    String htmlBody = buildForgotPasswordEmailTemplate(token);
    sendEmail(email, "Password Reset Request", htmlBody).subscribe();
  }

  private String buildForgotPasswordEmailTemplate(String token) {
    Context context = new Context();
    context.setVariable(
        "resetLink", appConfigurationProperties.getAppBaseUrl() + "/reset-password?token=" + token);
    return templateEngine.process("forgot-password-email-template", context);
  }


  @Async
  public void sendCostRequestLinePendingApprovalEmail(
      List<String> emails,
      String costRequestLinePartNumber,
      String costRequestLineRevision,
      String costRequestReferenceNumber,
      String costRequestRevision) {
    String subject = "Line pending approval";
    String htmlBody =
        buildCostRequestLinePendingApprovalEmailTemplate(
            costRequestLinePartNumber,
            costRequestLineRevision,
            costRequestReferenceNumber,
            costRequestRevision,
            subject);
    sendEmailToMultipleRecipients(emails, subject, htmlBody).subscribe();
  }

  private String buildCostRequestLinePendingApprovalEmailTemplate(
      String costRequestLinePartNumber,
      String costRequestLineRevision,
      String costRequestReferenceNumber,
      String costRequestRevision,
      String subject) {
    Context context = new Context();
    context.setVariable("crLinePartNumber", costRequestLinePartNumber);
    context.setVariable("crLineRevision", costRequestLineRevision);
    context.setVariable("crReferenceNumber", costRequestReferenceNumber);
    context.setVariable("crRevision", costRequestRevision);
    context.setVariable("title", subject);
    return templateEngine.process(
        "cost-request-line-pending-approval-email-template.html", context);
  }

  @Async
  public void sendAllMaterialEstimatedEmail(
      List<String> emails,
      String costRequestLinePartNumber,
      String costRequestLineRevision,
      String costRequestReferenceNumber,
      String costRequestRevision) {
    String subject = "All materials estimated";
    String htmlBody =
        buildAllMaterialEstimatedEmailTemplate(
            costRequestLinePartNumber,
            costRequestLineRevision,
            costRequestReferenceNumber,
            costRequestRevision,
            subject);
    sendEmailToMultipleRecipients(emails, subject, htmlBody).subscribe();
  }

  private String buildAllMaterialEstimatedEmailTemplate(
      String costRequestLinePartNumber,
      String costRequestLineRevision,
      String costRequestReferenceNumber,
      String costRequestRevision,
      String subject) {
    Context context = new Context();
    context.setVariable("crLinePartNumber", costRequestLinePartNumber);
    context.setVariable("crLineRevision", costRequestLineRevision);
    context.setVariable("crReferenceNumber", costRequestReferenceNumber);
    context.setVariable("crRevision", costRequestRevision);
    context.setVariable("title", subject);
    return templateEngine.process("all-material-estimated-email-template.html", context);
  }

  @Async
  public void sendCostRequestLineRevertedToReadyToEstimateEmail(
      List<String> emails,
      String costRequestLinePartNumber,
      String costRequestLineRevision,
      String costRequestReferenceNumber,
      String costRequestRevision) {
    String subject = "Line reverted to Ready to Estimate";
    String htmlBody =
        buildCostRequestLineRevertedToReadyToEstimateEmailTemplate(
            costRequestLinePartNumber,
            costRequestLineRevision,
            costRequestReferenceNumber,
            costRequestRevision,
            subject);
    sendEmailToMultipleRecipients(emails, subject, htmlBody).subscribe();
  }

  private String buildCostRequestLineRevertedToReadyToEstimateEmailTemplate(
      String costRequestLinePartNumber,
      String costRequestLineRevision,
      String costRequestReferenceNumber,
      String costRequestRevision,
      String subject) {
    Context context = new Context();
    context.setVariable("crLinePartNumber", costRequestLinePartNumber);
    context.setVariable("crLineRevision", costRequestLineRevision);
    context.setVariable("crReferenceNumber", costRequestReferenceNumber);
    context.setVariable("crRevision", costRequestRevision);
    context.setVariable("title", subject);
    return templateEngine.process(
        "cost-request-line-reverted-to-ready-to-estimate-email-template.html", context);
  }

  @Async
  public void sendNewMaterialToEstimateEmail(List<String> emails) {
    String subject = "New Materials to estimate";
    String htmlBody = buildNewMaterialToEstimateEmailTemplate(subject);
    sendEmailToMultipleRecipients(emails, subject, htmlBody).subscribe();
  }

  private String buildNewMaterialToEstimateEmailTemplate(String subject) {
    Context context = new Context();
    context.setVariable("title", subject);
    return templateEngine.process("new-material-to-estimate-email-template.html", context);
  }

  @Async
  public void sendNewCostRequestLineReadyToValidateEmail(
      List<String> emails,
      String costRequestLinePartNumber,
      String costRequestLineRevision,
      String costRequestReferenceNumber,
      String costRequestRevision) {
    String subject = "New line ready to validate";
    String htmlBody =
        buildNewCostRequestLineReadyToValidateEmailTemplate(
            costRequestLinePartNumber,
            costRequestLineRevision,
            costRequestReferenceNumber,
            costRequestRevision,
            subject);
    sendEmailToMultipleRecipients(emails, subject, htmlBody).subscribe();
  }

  private String buildNewCostRequestLineReadyToValidateEmailTemplate(
      String costRequestLinePartNumber,
      String costRequestLineRevision,
      String costRequestReferenceNumber,
      String costRequestRevision,
      String subject) {
    Context context = new Context();
    context.setVariable("crLinePartNumber", costRequestLinePartNumber);
    context.setVariable("crLineRevision", costRequestLineRevision);
    context.setVariable("crReferenceNumber", costRequestReferenceNumber);
    context.setVariable("crRevision", costRequestRevision);
    context.setVariable("title", subject);
    return templateEngine.process(
        "new-cost-request-line-ready-to-validate-email-template.html", context);
  }

  @Async
  public void sendNewCostRequestForReviewEmail(
      List<String> emails, String costRequestReferenceNumber, String costRequestRevision) {
    String subject = "New request for quotation for review";
    String htmlBody =
        buildNewCostRequestForReviewEmailTemplate(
            costRequestReferenceNumber, costRequestRevision, subject);
    sendEmailToMultipleRecipients(emails, subject, htmlBody).subscribe();
  }

  private String buildNewCostRequestForReviewEmailTemplate(
      String costRequestReferenceNumber, String costRequestRevision, String subject) {
    Context context = new Context();
    context.setVariable("crReferenceNumber", costRequestReferenceNumber);
    context.setVariable("crRevision", costRequestRevision);
    context.setVariable("title", subject);
    return templateEngine.process("new-cost-request-for-review-email-template.html", context);
  }

  @Async
  public void sendCostRequestNewMessageEmail(
      List<String> emails,
      String from,
      String costRequestReferenceNumber,
      String costRequestRevision) {
    String subject = buildNewMessageEmailSubject("Request for Quotation");
    String htmlBody =
        buildCostRequestNewMessageEmailTemplate(
            from, costRequestReferenceNumber, costRequestRevision, subject);
    sendEmailToMultipleRecipients(emails, subject, htmlBody).subscribe();
  }

  private String buildCostRequestNewMessageEmailTemplate(
      String from, String costRequestReferenceNumber, String costRequestRevision, String subject) {
    Context context = new Context();
    context.setVariable("crReferenceNumber", costRequestReferenceNumber);
    context.setVariable("crRevision", costRequestRevision);
    context.setVariable("title", subject);
    context.setVariable("login", from);
    return templateEngine.process("new-message-cost-request-email-template.html", context);
  }

  @Async
  public void sendCostRequestLineNewMessageEmail(
      List<String> emails,
      String from,
      String costRequestLinePartNumber,
      String costRequestLineRevision,
      String costRequestReferenceNumber,
      String costRequestRevision) {
    String subject = buildNewMessageEmailSubject("Outsourced P/N");
    String htmlBody =
        buildCostRequestLineNewMessageEmailTemplate(
            from,
            costRequestLinePartNumber,
            costRequestLineRevision,
            costRequestReferenceNumber,
            costRequestRevision,
            subject);
    sendEmailToMultipleRecipients(emails, subject, htmlBody).subscribe();
  }

  private String buildCostRequestLineNewMessageEmailTemplate(
      String from,
      String costRequestLinePartNumber,
      String costRequestLineRevision,
      String costRequestReferenceNumber,
      String costRequestRevision,
      String subject) {
    Context context = new Context();
    context.setVariable("crLinePartNumber", costRequestLinePartNumber);
    context.setVariable("crLineRevision", costRequestLineRevision);
    context.setVariable("crReferenceNumber", costRequestReferenceNumber);
    context.setVariable("crRevision", costRequestRevision);
    context.setVariable("title", subject);
    context.setVariable("login", from);
    return templateEngine.process(
        "new-message-outsourced-cost-request-line-email-template.html", context);
  }

  private String buildCostRequestToolingLineNewMessageToEngineeringEmailTemplate(
      String from,
      String toolingDescription,
      String costRequestLinePartNumber,
      String costRequestLineRevision,
      String costRequestReferenceNumber,
      String costRequestRevision,
      String subject) {
    Context context = new Context();
    context.setVariable("description", toolingDescription);
    context.setVariable("crLinePartNumber", costRequestLinePartNumber);
    context.setVariable("crLineRevision", costRequestLineRevision);
    context.setVariable("crReferenceNumber", costRequestReferenceNumber);
    context.setVariable("crRevision", costRequestRevision);
    context.setVariable("title", subject);
    context.setVariable("login", from);
    return templateEngine.process(
        "new-message-outsourced-tooling-line-to-engineering-email-template.html", context);
  }

  private String buildNewMessageEmailSubject(String prefix) {
    return prefix + ": new message";
  }
}
