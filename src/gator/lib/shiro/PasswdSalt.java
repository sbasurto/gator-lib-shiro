package gator.lib.shiro;

class PasswdSalt {
	public String password;
	public String salt;

	public PasswdSalt(String password, String salt) {
		super();
		this.password = password;
		this.salt = salt;
	}

}
