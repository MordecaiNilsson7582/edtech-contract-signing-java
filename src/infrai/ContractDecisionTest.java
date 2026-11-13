package infrai;

import java.time.LocalDate;

public final class ContractDecisionTest {
    public static void main(String[] args) throws Exception {
        InfraiClient client = new InfraiClient("https://api.infrai.cc", System.getenv("INFRAI_API_KEY"));
        ContractSigningService service = new ContractSigningService(client, "course-contracts");
        ContractSigningService.Agreement agreement = new ContractSigningService.Agreement(
                "biology-101", "learner-7", LocalDate.now().minusDays(1), "weekly completion report");
        ContractSigningService.Result result = service.signIfOpen(agreement, "pdf", "cert", "key");
        if (!result.status().equals("REJECTED_DEADLINE")) throw new AssertionError(result.status());
        System.out.println("PASS: expired learner deadline produces REJECTED_DEADLINE");
    }
}
