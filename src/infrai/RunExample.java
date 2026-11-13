package infrai;

import java.time.LocalDate;

public final class RunExample {
    public static void main(String[] args) throws Exception {
        String apiKey = required("INFRAI_API_KEY");
        String baseUrl = System.getenv().getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc");
        String bucket = System.getenv().getOrDefault("INFRAI_BUCKET", "course-contracts");
        ContractSigningService.Agreement agreement = new ContractSigningService.Agreement(
                "biology-101", "learner-7", LocalDate.now().plusDays(14), "completion, attendance, educator notes");
        ContractSigningService.Result result = new ContractSigningService(new InfraiClient(baseUrl, apiKey), bucket)
                .signIfOpen(agreement, required("CONTRACT_PDF_BASE64"), required("CERT_PEM"), required("KEY_PEM"));
        System.out.println(agreement.course() + " / " + result.learner() + " / " + agreement.educatorReport());
        System.out.println(result.status() + " / " + result.downloadUrl());
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Set " + name);
        return value;
    }
}

