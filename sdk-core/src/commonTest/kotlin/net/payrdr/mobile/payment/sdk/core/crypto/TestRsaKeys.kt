package net.payrdr.mobile.payment.sdk.core.crypto

/** A key pair made for these tests only, so a test can open the token the SDK encrypts. */
internal object TestRsaKeys {
    val PUBLIC_KEY_PEM =
        """
        -----BEGIN PUBLIC KEY-----
        MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqHRXDoMZmcPIENi/0eT0
        nKB6uyWon4U4Wvdiin/bEECgf9MQAkqqXlpiP983Q4Im35eLlSvV1TGyv0hPwU1a
        hizT2/cBRupaSFwhd68aaRiHFV+ZgZeEdQKKBX4FN1kkhV5Lc33FULnGCNzJ80m+
        9HDQI5zWu1BmwkUa5s6sWmU0zJDKs2qkQqiaANmM+mN2pgoY8yB4K4TfSlnzxTDF
        S50vD89RBQe137EdnnT9yNHPxCbqHtzX6QkkEKO41Lc9SdkT+6wBZ+m2isl5Usvw
        mrEbAYQz7rfdi9CMACJ2FyqmqCRp1pHrIuTAzGAFoXG2ekp3Kanxf2QD3tCW7+oX
        EwIDAQAB
        -----END PUBLIC KEY-----
        """.trimIndent()

    /** The private key as PKCS#8 DER in Base64, without PEM armour. */
    val PRIVATE_KEY_PKCS8_BASE64 =
        """
        MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCodFcOgxmZw8gQ2L/R5PScoHq7JaifhTha92KKf9sQQKB/0xAC
        SqpeWmI/3zdDgibfl4uVK9XVMbK/SE/BTVqGLNPb9wFG6lpIXCF3rxppGIcVX5mBl4R1AooFfgU3WSSFXktzfcVQucYI3MnzSb70
        cNAjnNa7UGbCRRrmzqxaZTTMkMqzaqRCqJoA2Yz6Y3amChjzIHgrhN9KWfPFMMVLnS8Pz1EFB7XfsR2edP3I0c/EJuoe3NfpCSQQ
        o7jUtz1J2RP7rAFn6baKyXlSy/CasRsBhDPut92L0IwAInYXKqaoJGnWkesi5MDMYAWhcbZ6SncpqfF/ZAPe0Jbv6hcTAgMBAAEC
        ggEACkB+VdtDsRh5shDG58rD2k5n+cDKeN7eNacNn7k1R1Fvkyfwtvr9UF220gVPMzQWEpCCBbOTwXRTWRHZDL4c7sKGqEHpmXvE
        v9efj82f0VqVSgc16+2hOY/Mf5YSUjKCJv28oMw8OHP96ecqATzxOqYs/KXcaaz0fAlBoF9enOennoKnSj8F3s3oLPnweFkcGafh
        Yxe720mABr3KJl6G4AtJSD+9HSWtuirZ6mvNYjlDgvBJK9f2vSRmbpwXxLYCy8XSqhU/m/YJByJVz5nzHyHnBJd+Vaw/8m2OLzG7
        +LCULoZnr0LLc/+Yl2WxHCbTPgICfy21PI7LMBhq7MspLQKBgQDQ2t5xD2eDEQ5UGbOCpoqve3RtZNSIIXNLjcBBARvB2NHqXl8+
        alY6BAETGJZ6YTYAMLKRI+kzL8h87H6NM8XeYl3bSWNfgkpaaz7Y0Sy3yIcDrdOP0mSe1umUymeDk9Hjj9pvHmIFZjcHXmuXJAbb
        8wSGl5gRsVnOA7iNlc3dZQKBgQDOetdt9iX5cUm5Bn7PnMFanP6sXCDv8RP2wgkpZp45bE0Pg5ObjJ48jMzCIyvmeMz2Cqh9jqG0
        5C7ed7Y8O6RI3ueyhlKqOj2TT5/gJj0Ryp8Gkzthcb/WZTeKyUN9QLhPG1UJqfCAXu8XK+QIMJe5tXATmm7Ja2LlyvHYbgi3FwKB
        gQDNuora+72PEcH3fcmhrMN30DgDegsQfULA7cSvQ5RAAn/kSUmwhbaiN0eHs/SmYBN9ZHrBFYqk7TCkPaD7HkCOqn6hf0zvy7vK
        DVI+6O0Pq+uy6As55JlRDYnREv+cUIOzkQQt6NPNVb5m988rdkV7a4rNuY4F/5jioi+s6B99MQKBgQCDCTRkhXzuA7ZMpNGfghS3
        I8D/s2v3uh/4ToWv737kZFW3TcJESgY12Qks/1Mh7XHthzAMlpMLEHCqIIDXUuZclJGoG1Sk0Vc4gKSuhLXm75bifleF7MFZF10b
        R8T73THaijb6bg3B+f1z0BfkFqqDYaDh3/UFAJB/0r6Ooq4HOwKBgQCrHpVLKhR1UtHei8xGjdnu2YrNPw4+fJqBq8ejZc6pEJOt
        vJ/d0KjxUqKu/Hhnp5T3Up/Jd7uIdnMVvWmi77rPHWgvhpM2UHr6Fa3IyoJelfDPmhCbq2BlVZG1wihmYHlOsmvxXYnVf/hM3cID
        z+Bda8gUf54aETLsP7lsTEn1sQ==
        """.filterNot {
            it.isWhitespace()
        }
}
