package Authentication.Controller;

/**
 * @author Robin Duda
 */
interface Request {

    boolean authorized();

    void error();

    void unauthorized();

    void write(Object object);

    void accept();

    void missing();

    void conflict();
}