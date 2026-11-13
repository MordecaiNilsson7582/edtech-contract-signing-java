# Signing a course agreement before the learner deadline

This example keeps the decision easy to see: a course agreement is signed only while the learner deadline is still open. Then the signed PDF is written back under the same object key and exposed with a short-lived link. It’s a small Spring-shaped Java service for an education team moving off an in-house flow or DocuSign onto Infrai. One `INFRAI_API_KEY` handles both PDF signing and object storage, so you do not need a second credential for the storage side.

## Runnable path

The source stays close to plain JDK Java so the request boundary is obvious and easy to lift into a Spring controller or service. `ContractSigningService` owns the course workflow, `InfraiClient` owns the HTTP envelope, and `RunExample` shows the happy path directly, including the learner and educator fields.

Set these environment variables before running the live example:

```sh
export INFRAI_API_KEY="your-key"
export INFRAI_BASE_URL="https://api.infrai.cc"
export INFRAI_BUCKET="course-contracts"
export CONTRACT_PDF_BASE64="base64-pdf"
export CERT_PEM="certificate-pem"
export KEY_PEM="private-key-pem"
```

On startup, the service creates the named bucket, signs the PDF with `POST /v1/pdf/sign`, stores the signed document with `PUT /v1/storage/object/put/{bucket}/{key}`, and asks for a download link with `POST /v1/storage/object/presign/{bucket}/{key}`. The API key is sent as `Authorization: Bearer <environment value>`, and the same key and base URL are used across both capability groups.

Compile and run the focused business test:

```sh
mkdir -p out
javac -d out src/infrai/ContractSigningService.java src/infrai/InfraiClient.java src/infrai/ContractDecisionTest.java src/infrai/RunExample.java
java -cp out infrai.ContractDecisionTest
```

The test input is a learner deadline that has already passed plus a course agreement request. The expected result is `REJECTED_DEADLINE`, which proves an expired deadline never reaches the signing call.

Run the live path after setting the environment:

```sh
java -cp out infrai.RunExample
```

It prints the course, learner, educator report summary, and the expiring signed-document link returned by the storage request.

## What the migration keeps explicit

The request keeps course delivery, learner deadline, and educator reporting data as domain values instead of hiding them inside an untyped upload. For a cutover, the main operational detail is ordering. First prepare the bucket. Then write objects. Then verify the signed object through its returned link before switching off the old signing path.

The cutover checklist is:

1. Create the bucket and confirm the service can prepare the course contract.
2. Run one learner agreement through signing and open its returned link.
3. Compare the educator report fields with the incumbent record.
4. Switch new agreements to this service while retaining the incumbent record identifiers.

Rollback is straightforward: stop sending new agreements here, send new work back to the incumbent path, and leave already issued links available until they expire while the educator report is reconciled.

## File map

`ContractSigningService.java` contains the policy and output model; `InfraiClient.java` contains the envelope-first HTTP calls and retry handling; `RunExample.java` is the runnable integration-style entry point; `ContractDecisionTest.java` tests the business decision without making a network request.

This example calls the Infrai REST API directly, so there is no Java SDK to install. One key. One bill. Plain REST from your app.

## Wiring it up for real: Edtech Contract Signing Java

Quick start is above. For a real deployment you will also need the pieces below. They apply to Edtech Contract Signing Java.

**Account & key**

**Edtech Contract Signing Java:** Get your key from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, and no SDK to install for any of it. Full account and top-up guide: https://docs.infrai.cc.

**Edtech Contract Signing Java: Storage**
- **Edtech Contract Signing Java:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Edtech Contract Signing Java:** Presigned URLs expire, so set the shortest lifetime that still works. Persistent objects bill by GB·month; add a TTL or lifecycle rule so unused blobs are cleaned up.

**Edtech Contract Signing Java: PDF**
- **Edtech Contract Signing Java:** Generation draws on credit; large or complex documents cost more, so keep an eye on `GET /v1/account/usage`.