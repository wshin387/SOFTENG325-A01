package se325.assignment01.concert.webapp;

import se325.assignment01.concert.webapp.util.AuthUtil;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ConcertsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        AuthUtil.setSignedInStatus(req);

        req.getRequestDispatcher("/WEB-INF/jsp/concerts.jsp").forward(req, resp);

    }
}
