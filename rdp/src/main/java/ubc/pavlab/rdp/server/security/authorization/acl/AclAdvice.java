package ubc.pavlab.rdp.server.security.authorization.acl;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import gemma.gsec.acl.BaseAclAdvice;
import gemma.gsec.model.Securable;

@Component
public class AclAdvice extends BaseAclAdvice {

	@Override
	protected GrantedAuthority getUserGroupGrantedAuthority(Securable arg0) {
		return null;
	}

	@Override
	protected String getUserName(Securable arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean objectIsUser(Securable arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean objectIsUserGroup(Securable arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
