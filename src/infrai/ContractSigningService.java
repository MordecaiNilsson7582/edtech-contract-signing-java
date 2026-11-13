package infrai;

import java.time.LocalDate;

public final class ContractSigningService {
    public record Agreement(String course, String learner, LocalDate deadline, String educatorReport) {}
    public record Result(String status, String learner, String downloadUrl) {}

    private final InfraiClient client;
    private final String bucket;

    public ContractSigningService(InfraiClient client, String bucket) { this.client = client; this.bucket = bucket; }

    public Result signIfOpen(Agreement agreement, String pdfBase64, String certPem, String keyPem) throws Exception {
        if (agreement.deadline().isBefore(LocalDate.now())) return new Result("REJECTED_DEADLINE", agreement.learner(), "");
        client.createBucket(bucket);
        byte[] signedPdf = client.sign(pdfBase64, certPem, keyPem);
        String key = agreement.course() + "/" + agreement.learner() + ".pdf";
        client.putObject(bucket, key, signedPdf);
        return new Result("SIGNED", agreement.learner(), client.presignGet(bucket, key));
    }
}

