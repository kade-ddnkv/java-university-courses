package root.integrat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountManagerTest {

    @Spy
    AccountManager am;

    @Mock
    IServer server;

    @Test
    void callLoginSucceed() {
        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 321L));
        am.init(server);

        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(321L, res.response);
    }

    @Test
    void callLoginSucceedMultipleUsersSimultaneously() {
        doReturn("hash1").when(am).makeSecure("pass1");
        doReturn("hash2").when(am).makeSecure("pass2");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 321L));
        when(server.login("user2", "hash2")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 432L));
        am.init(server);

        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(321L, res.response);

        res = am.callLogin("user2", "pass2");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(432L, res.response);
    }

    @Test
    void callLoginAlreadyLogged() {
        doReturn("hash1").when(am).makeSecure("pass1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, 322L)).when(server).login("user1", "hash1");
        am.init(server);

        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertEquals(322L, res.response);
        res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.ALREADY_LOGGED, res.code);
        assertNull(res.response);
    }

    @Test
    void callLoginNoUser() {
        doReturn("hash1").when(am).makeSecure("pass1");
        doReturn(new ServerResponse(ServerResponse.NO_USER_INCORRECT_PASSWORD, null)).when(server).login("wrong_user1", "hash1");
        am.init(server);

        AccountManagerResponse res = am.callLogin("wrong_user1", "pass1");
        assertEquals(AccountManagerResponse.NO_USER_INCORRECT_PASSWORD, res.code);
        assertNull(res.response);
    }

    @Test
    void callLoginIncorrectPassword() {
        doReturn("wrong_hash1").when(am).makeSecure("wrong_pass1");
        doReturn(new ServerResponse(ServerResponse.NO_USER_INCORRECT_PASSWORD, null)).when(server).login("user1", "wrong_hash1");
        am.init(server);

        AccountManagerResponse res = am.callLogin("user1", "wrong_pass1");
        assertEquals(AccountManagerResponse.NO_USER_INCORRECT_PASSWORD, res.code);
        assertNull(res.response);
    }

    @Test
    void callLoginUndefinedError() {
        doReturn("hash1").when(am).makeSecure("pass1");
        doReturn(new ServerResponse(ServerResponse.UNDEFINED_ERROR, null)).when(server).login("user1", "hash1");
        am.init(server);

        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.UNDEFINED_ERROR, ((ServerResponse) res.response).code);
        assertNull(((ServerResponse) res.response).response);
    }

    @Test
    void callLogoutSucceed() {
        doReturn("hash1").when(am).makeSecure("pass1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, 321L)).when(server).login("user1", "hash1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(321L);
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(321L, res.response);

        res = am.callLogout("user1", 321L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void callLogoutSucceedMultipleUsersSimultaneously() {
        doReturn("hash1").when(am).makeSecure("pass1");
        doReturn("hash2").when(am).makeSecure("pass2");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 321L));
        when(server.login("user2", "hash2")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 432L));
        when(server.logout(321L)).thenReturn(new ServerResponse(ServerResponse.SUCCESS, null));
        when(server.logout(432L)).thenReturn(new ServerResponse(ServerResponse.SUCCESS, null));
        am.init(server);

        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(321L, res.response);

        res = am.callLogin("user2", "pass2");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(432L, res.response);

        res = am.callLogout("user1", 321L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);

        res = am.callLogout("user2", 432L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void callLogoutSucceedThanLoginSucceedDifferentSession() {
        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1"))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 111L))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 222L));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);

        res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(222L, res.response);
    }

    @Test
    void callLogoutNotLogged() {
        am.init(server);
        AccountManagerResponse res = am.callLogout("user1", 321L);
        assertEquals(AccountManagerResponse.NOT_LOGGED, res.code);
        assertNull(res.response);
    }

    @Test
    void callLogoutIncorrectSession() {
        doReturn("hash1").when(am).makeSecure("pass1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, 321L)).when(server).login("user1", "hash1");
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(321L, res.response);

        res = am.callLogout("user1", 666L);
        assertEquals(AccountManagerResponse.INCORRECT_SESSION, res.code);
        assertNull(res.response);
    }

    @Test
    void callLogoutIncorrectSessionThenSucceed() {
        doReturn("hash1").when(am).makeSecure("pass1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, 321L)).when(server).login("user1", "hash1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(321L);
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(321L, res.response);

        res = am.callLogout("user1", 666L);
        assertEquals(AccountManagerResponse.INCORRECT_SESSION, res.code);
        assertNull(res.response);

        res = am.callLogout("user1", 321L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void callLogoutUndefinedError() {
        doReturn("hash1").when(am).makeSecure("pass1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, 321L)).when(server).login("user1", "hash1");
        doReturn(new ServerResponse(ServerResponse.UNDEFINED_ERROR, null)).when(server).logout(321L);
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(321L, res.response);

        res = am.callLogout("user1", 321L);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.UNDEFINED_ERROR, ((ServerResponse) res.response).code);
        assertNull(((ServerResponse) res.response).response);
    }

    @Test
    void callLogoutWrongResponseFromServer() {
        // Когда logout от IServer возвращает то, что по ТЗ возвращать не должен.
        // Это коды NO_MONEY, NO_USER_INCORRECT_PASSWORD, ALREADY_LOGGED.
        // AccountManager на это должен выдавать UNDEFINED_ERROR.

        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 321L));
        when(server.logout(321L))
                .thenReturn(new ServerResponse(ServerResponse.NO_MONEY, 10.1))
                .thenReturn(new ServerResponse(ServerResponse.NO_USER_INCORRECT_PASSWORD, null))
                .thenReturn(new ServerResponse(ServerResponse.ALREADY_LOGGED, null));
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(321L, res.response);

        res = am.callLogout("user1", 321L);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.NO_MONEY, ((ServerResponse) res.response).code);
        assertTrue(((ServerResponse) res.response).response instanceof Double);
        assertEquals(10.1, ((ServerResponse) res.response).response);

        res = am.callLogout("user1", 321L);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.NO_USER_INCORRECT_PASSWORD, ((ServerResponse) res.response).code);
        assertNull(((ServerResponse) res.response).response);

        res = am.callLogout("user1", 321L);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.ALREADY_LOGGED, ((ServerResponse) res.response).code);
        assertNull(((ServerResponse) res.response).response);
    }

    @Test
    void getBalanceSucceedThanLogoutSucceed() {
        doReturn("hash1").when(am).makeSecure("pass1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, 432L)).when(server).login("user1", "hash1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(432L);
        doReturn(new ServerResponse(ServerResponse.SUCCESS, 90.1)).when(server).getBalance(432L);
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(432L, res.response);

        res = am.getBalance("user1", 432L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Double);
        assertEquals(90.1, res.response);

        res = am.callLogout("user1", 432L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void getBalanceNotLogged() {
        doReturn("hash1").when(am).makeSecure("pass1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, 432L)).when(server).login("user1", "hash1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(432L);
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(432L, res.response);

        // Другой пользователь, та же сессия
        res = am.getBalance("user2", 432L);
        assertEquals(AccountManagerResponse.NOT_LOGGED, res.code);
        assertNull(res.response);

        // Другой пользователь, другая сессия
        res = am.getBalance("user2", 666L);
        assertEquals(AccountManagerResponse.NOT_LOGGED, res.code);
        assertNull(res.response);

        res = am.callLogout("user1", 432L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void getBalanceIncorrectSession() {
        doReturn("hash1").when(am).makeSecure("pass1");
        doReturn("hash2").when(am).makeSecure("pass2");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, 111L)).when(server).login("user1", "hash1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, 222L)).when(server).login("user2", "hash2");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(222L);
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        res = am.getBalance("user1", 222L);
        assertEquals(AccountManagerResponse.INCORRECT_SESSION, res.code);
        assertNull(res.response);

        // Логиню user2 с сессией 222 и пробую getBalance("user1", 222L)
        res = am.callLogin("user2", "pass2");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(222L, res.response);
        res = am.getBalance("user1", 222L);
        assertEquals(AccountManagerResponse.INCORRECT_SESSION, res.code);
        assertNull(res.response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);

        res = am.callLogout("user2", 222L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void getBalanceUndefinedError() {
        doReturn("hash1").when(am).makeSecure("pass1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, 111L)).when(server).login("user1", "hash1");
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        doReturn(new ServerResponse(ServerResponse.UNDEFINED_ERROR, null)).when(server).getBalance(111L);
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        res = am.getBalance("user1", 111L);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.UNDEFINED_ERROR, ((ServerResponse) res.response).code);
        assertNull(((ServerResponse) res.response).response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void getBalanceWrongResponsesFromServer() {
        // Посылаю невозможные по ТЗ ответы от сервера.
        // Например, deposit вдруг вернет NO_MONEY, NO_USER_INCORRECT_PASSWORD, ALREADY_LOGGED,
        // хотя по ТЗ IServer, он этого делать не может.
        // И AccountManager на это должен ответить UNDEFINED_ERROR.
        // (Вообще лучше INCORRECT_RESPONSE, но в ТЗ написано по-другому)
        // Не уверен, нужно ли это проверять.

        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 111L));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        when(server.getBalance(111L))
                .thenReturn(new ServerResponse(ServerResponse.NO_MONEY, 10.1))
                .thenReturn(new ServerResponse(ServerResponse.NO_USER_INCORRECT_PASSWORD, null))
                .thenReturn(new ServerResponse(ServerResponse.ALREADY_LOGGED, null));
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        res = am.getBalance("user1", 111L);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.NO_MONEY, ((ServerResponse) res.response).code);
        assertTrue(((ServerResponse) res.response).response instanceof Double);
        assertEquals(10.1, ((ServerResponse) res.response).response);

        res = am.getBalance("user1", 111L);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.NO_USER_INCORRECT_PASSWORD, ((ServerResponse) res.response).code);
        assertNull(((ServerResponse) res.response).response);

        res = am.getBalance("user1", 111L);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.ALREADY_LOGGED, ((ServerResponse) res.response).code);
        assertNull(((ServerResponse) res.response).response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void depositSucceedThenReloginDifferentSessionThenGetBalance() {
        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1"))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 111L))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 222L));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        when(server.getBalance(111L))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 1.1))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 29001.05));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, 29001.05)).when(server).deposit(111L, 28999.95);
        when(server.getBalance(222L)).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 29001.05));
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        res = am.getBalance("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertEquals(1.1, res.response);

        res = am.deposit("user1", 111L, 28999.95);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Double);
        assertEquals(29001.05, res.response);

        res = am.getBalance("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Double);
        assertEquals(29001.05, res.response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);

        res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(222L, res.response);

        res = am.getBalance("user1", 222L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Double);
        assertEquals(29001.05, res.response);
    }

    @Test
    void depositNotLogged() {
        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 111L));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        // Другой пользователь, та же сессия
        res = am.deposit("user2", 111L, 28999.95);
        assertEquals(AccountManagerResponse.NOT_LOGGED, res.code);
        assertNull(res.response);

        // Другой пользователь, другая сессия.
        res = am.deposit("user2", 222L, 28999.95);
        assertEquals(AccountManagerResponse.NOT_LOGGED, res.code);
        assertNull(res.response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void depositIncorrectSession() {
        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 111L));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        res = am.deposit("user1", 222L, 28999.95);
        assertEquals(AccountManagerResponse.INCORRECT_SESSION, res.code);
        assertNull(res.response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void depositUndefinedError() {
        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 111L));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        when(server.deposit(111L, 28999.95)).thenReturn(new ServerResponse(ServerResponse.UNDEFINED_ERROR, null));
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        res = am.deposit("user1", 111L, 28999.95);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.UNDEFINED_ERROR, ((ServerResponse) res.response).code);
        assertNull(((ServerResponse) res.response).response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void depositWrongResponsesFromServer() {
        // Посылаю невозможные по ТЗ ответы от сервера.
        // Например, deposit вдруг вернет NO_MONEY, NO_USER_INCORRECT_PASSWORD, ALREADY_LOGGED,
        // хотя по ТЗ IServer, он этого делать не может.
        // И AccountManager на это должен ответить UNDEFINED_ERROR.

        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 111L));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        when(server.deposit(111L, 28999.95))
                .thenReturn(new ServerResponse(ServerResponse.NO_MONEY, 10.1))
                .thenReturn(new ServerResponse(ServerResponse.NO_USER_INCORRECT_PASSWORD, null))
                .thenReturn(new ServerResponse(ServerResponse.ALREADY_LOGGED, null));
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        res = am.deposit("user1", 111L, 28999.95);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.NO_MONEY, ((ServerResponse) res.response).code);
        assertTrue(((ServerResponse) res.response).response instanceof Double);
        assertEquals(10.1, ((ServerResponse) res.response).response);

        res = am.deposit("user1", 111L, 28999.95);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.NO_USER_INCORRECT_PASSWORD, ((ServerResponse) res.response).code);
        assertNull(((ServerResponse) res.response).response);

        res = am.deposit("user1", 111L, 28999.95);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.ALREADY_LOGGED, ((ServerResponse) res.response).code);
        assertNull(((ServerResponse) res.response).response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void withdrawSucceedThenReloginDifferentSessionThenGetBalance() {
        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1"))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 111L))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 222L));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        when(server.getBalance(111L))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 9000.12))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 8998.80));
        when(server.withdraw(111L, 1.32)).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 8998.80));
        when(server.getBalance(222L)).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 8998.80));
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        res = am.getBalance("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertEquals(9000.12, res.response);

        res = am.withdraw("user1", 111L, 1.32);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Double);
        assertEquals(8998.80, res.response);

        res = am.getBalance("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Double);
        assertEquals(8998.80, res.response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);

        res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(222L, res.response);

        res = am.getBalance("user1", 222L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Double);
        assertEquals(8998.80, res.response);
    }

    @Test
    void withdrawNotLogged() {
        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 111L));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        // Другой пользователь, та же сессия
        res = am.withdraw("user2", 111L, 90234.95);
        assertEquals(AccountManagerResponse.NOT_LOGGED, res.code);
        assertNull(res.response);

        // Другой пользователь, другая сессия.
        res = am.withdraw("user2", 222L, 90234.95);
        assertEquals(AccountManagerResponse.NOT_LOGGED, res.code);
        assertNull(res.response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void withdrawIncorrectSession() {
        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 111L));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        res = am.withdraw("user1", 222L, 90234.95);
        assertEquals(AccountManagerResponse.INCORRECT_SESSION, res.code);
        assertNull(res.response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void withdrawNoMoney() {
        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 111L));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        when(server.withdraw(111L, 999999.23232)).thenReturn(new ServerResponse(ServerResponse.NO_MONEY, 8.09));
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        res = am.withdraw("user1", 111L, 999999.23232);
        assertEquals(AccountManagerResponse.NO_MONEY, res.code);
        assertTrue(res.response instanceof Double);
        assertEquals(8.09, res.response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void withdrawUndefinedError() {
        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 111L));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        when(server.withdraw(111L, 999999.23232)).thenReturn(new ServerResponse(ServerResponse.UNDEFINED_ERROR, null));
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        res = am.withdraw("user1", 111L, 999999.23232);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.UNDEFINED_ERROR, ((ServerResponse) res.response).code);
        assertNull(((ServerResponse) res.response).response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void withdrawWrongResponsesFromServer() {
        // Посылаю невозможные по ТЗ ответы от сервера.
        // Например, withdraw вдруг вернет NO_USER_INCORRECT_PASSWORD или ALREADY_LOGGED,
        // хотя по ТЗ IServer, он этого делать не может.
        // И AccountManager на это должен ответить UNDEFINED_ERROR.

        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1")).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 111L));
        doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(server).logout(111L);
        when(server.withdraw(111L, 999999.23232))
                .thenReturn(new ServerResponse(ServerResponse.NO_USER_INCORRECT_PASSWORD, null))
                .thenReturn(new ServerResponse(ServerResponse.ALREADY_LOGGED, null));
        am.init(server);
        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(111L, res.response);

        res = am.withdraw("user1", 111L, 999999.23232);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.NO_USER_INCORRECT_PASSWORD, ((ServerResponse) res.response).code);
        assertNull(((ServerResponse) res.response).response);

        res = am.withdraw("user1", 111L, 999999.23232);
        assertEquals(AccountManagerResponse.UNDEFINED_ERROR, res.code);
        assertTrue(res.response instanceof ServerResponse);
        assertEquals(ServerResponse.ALREADY_LOGGED, ((ServerResponse) res.response).code);
        assertNull(((ServerResponse) res.response).response);

        res = am.callLogout("user1", 111L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }

    @Test
    void scenario1() {
        // Сценарий 1. Пользователь (user) проводит попытку авторизации в системе управления
        // аккаунтами с указанием некорректного логина. После этого он проводит вторую попытку
        // авторизации с указанием корректного логина и неправильного пароля. С третьей попытки
        // пользователь авторизуется и делает запрос баланса. После получения значения проводится
        // попытка внести на счет 100 единиц. Полученное значение сравнивается с ожидаемым.

        doReturn("wrong_hash1").when(am).makeSecure("wrong_pass1");
        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("wrong_user1", "hash1"))
                .thenReturn(new ServerResponse(ServerResponse.NO_USER_INCORRECT_PASSWORD, null));
        when(server.login("user1", "wrong_hash1"))
                .thenReturn(new ServerResponse(ServerResponse.NO_USER_INCORRECT_PASSWORD, null));
        when(server.login("user1", "hash1"))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 123L));
        when(server.getBalance(123L)).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 95.1));
        when(server.deposit(123L, 100)).thenReturn(new ServerResponse(ServerResponse.SUCCESS, 195.1));

        am.init(server);
        AccountManagerResponse res = am.callLogin("wrong_user1", "pass1");
        assertEquals(AccountManagerResponse.NO_USER_INCORRECT_PASSWORD, res.code);
        assertNull(res.response);

        res = am.callLogin("user1", "wrong_pass1");
        assertEquals(AccountManagerResponse.NO_USER_INCORRECT_PASSWORD, res.code);
        assertNull(res.response);

        res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(123L, res.response);

        res = am.getBalance("user1", 123L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Double);
        assertEquals(95.1, res.response);

        res = am.deposit("user1", 123L, 100);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Double);
        assertEquals(195.1, res.response);
    }

    @Test
    void scenario2() {
        // Сценарий 2. Пользователь (user) проводит успешную авторизацию. Проводится попытка
        // снятия 50 единиц (неудачная). Делается запрос на количество средств на счету. Проводится
        // внесение 100 единиц. Проводится снятие 50 единиц с указанием некорректного номера
        // сессии (неудачное). Проводится снятие 50 единиц с правильным номером сессии.
        // Проводится выход из системы (logout).

        doReturn("hash1").when(am).makeSecure("pass1");
        when(server.login("user1", "hash1"))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 5L));
        when(server.withdraw(5L, 50))
                .thenReturn(new ServerResponse(ServerResponse.NO_MONEY, 3.3))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 53.3));
        when(server.deposit(5L, 100))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, 103.3));
        when(server.logout(5L))
                .thenReturn(new ServerResponse(ServerResponse.SUCCESS, null));
        am.init(server);

        AccountManagerResponse res = am.callLogin("user1", "pass1");
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Long);
        assertEquals(5L, res.response);

        res = am.withdraw("user1", 5L, 50);
        assertEquals(AccountManagerResponse.NO_MONEY, res.code);
        assertTrue(res.response instanceof Double);
        assertEquals(3.3, res.response);

        res = am.deposit("user1", 5L, 100);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Double);
        assertEquals(103.3, res.response);

        res = am.withdraw("user1", 9L, 50);
        assertEquals(AccountManagerResponse.INCORRECT_SESSION, res.code);
        assertNull(res.response);

        res = am.withdraw("user1", 5L, 50);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertTrue(res.response instanceof Double);
        assertEquals(53.3, res.response);

        res = am.callLogout("user1", 5L);
        assertEquals(AccountManagerResponse.SUCCEED, res.code);
        assertNull(res.response);
    }
}