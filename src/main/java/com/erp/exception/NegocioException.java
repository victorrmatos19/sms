package com.erp.exception;

/**
 * Exceção de regra de negócio — lançada pelos Services e capturada nos Controllers
 * para exibir um Alert de erro ao usuário sem stack trace desnecessário.
 */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensagem) {
        super(mensagem);
    }
}
