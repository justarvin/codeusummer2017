package codeu.chat;

import codeu.chat.util.PasswordUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Test class for password authentication
 */
public class AuthTest {

  @Test
  public void testWrongPassword() {
    boolean pass = true;
    try {
      String correctHashed = PasswordUtils.createHash("test");
      String testPassword = "incorrect";
      pass = PasswordUtils.verifyPassword(testPassword, correctHashed);
    } catch (Exception e) {
      e.printStackTrace();
    }
    assertEquals(false, pass);
  }

  @Test
  public void testCorrectPassword() {
    boolean pass = false;
    try {
      String correctHashed = PasswordUtils.createHash("test");
      String testPassword = "test";
      pass = PasswordUtils.verifyPassword(testPassword, correctHashed);
    } catch (Exception e) {
      e.printStackTrace();
    }
    assertEquals(true, pass);
  }

  @Test
  public void testEncoding() {
    String hashed = "";
    String pass = "rawPassword";
    try {
      hashed = PasswordUtils.createHash(pass);
    } catch (PasswordUtils.CannotPerformOperationException e) {
      e.printStackTrace();
    }
    assertNotEquals(pass, hashed);
  }
}
