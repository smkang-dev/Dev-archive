package com.devarchive.service;

public class StaleRevisionException extends RuntimeException {
  public StaleRevisionException() {
    super("다른 화면에서 자료가 변경되었습니다. 최신 화면을 연 뒤 다시 수정해 주세요.");
  }
}
