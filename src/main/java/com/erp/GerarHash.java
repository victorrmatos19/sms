package com.erp.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilitário temporário para gerar hash bcrypt.
 * Execute com: mvn compile exec:java -Dexec.mainClass="com.erp.util.GerarHash"
 */
public class GerarHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String[] senhas = {"admin123", "admin", "erp123"};
        for (String senha : senhas) {
            System.out.println(senha + " => " + encoder.encode(senha));
        }
    }
}