package com.dianrong.common.uniauth.server.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.dianrong.common.uniauth.common.bean.InfoName;
import com.dianrong.common.uniauth.common.bean.dto.DomainDto;
import com.dianrong.common.uniauth.common.bean.dto.PageDto;
import com.dianrong.common.uniauth.common.bean.dto.PermissionDto;
import com.dianrong.common.uniauth.common.bean.dto.RoleDto;
import com.dianrong.common.uniauth.common.bean.dto.TagDto;
import com.dianrong.common.uniauth.common.bean.dto.UserDetailDto;
import com.dianrong.common.uniauth.common.bean.dto.UserDto;
import com.dianrong.common.uniauth.common.bean.dto.UserExtendValDto;
import com.dianrong.common.uniauth.common.bean.request.LoginParam;
import com.dianrong.common.uniauth.common.bean.request.UserParam;
import com.dianrong.common.uniauth.common.cons.AppConstants;
import com.dianrong.common.uniauth.common.enm.UserActionEnum;
import com.dianrong.common.uniauth.common.util.AuthUtils;
import com.dianrong.common.uniauth.common.util.Base64;
import com.dianrong.common.uniauth.common.util.UniPasswordEncoder;
import com.dianrong.common.uniauth.server.data.entity.Domain;
import com.dianrong.common.uniauth.server.data.entity.DomainExample;
import com.dianrong.common.uniauth.server.data.entity.Grp;
import com.dianrong.common.uniauth.server.data.entity.GrpExample;
import com.dianrong.common.uniauth.server.data.entity.GrpPath;
import com.dianrong.common.uniauth.server.data.entity.GrpPathExample;
import com.dianrong.common.uniauth.server.data.entity.PermType;
import com.dianrong.common.uniauth.server.data.entity.Permission;
import com.dianrong.common.uniauth.server.data.entity.PermissionExample;
import com.dianrong.common.uniauth.server.data.entity.Role;
import com.dianrong.common.uniauth.server.data.entity.RoleCode;
import com.dianrong.common.uniauth.server.data.entity.RoleCodeExample;
import com.dianrong.common.uniauth.server.data.entity.RoleExample;
import com.dianrong.common.uniauth.server.data.entity.RolePermissionExample;
import com.dianrong.common.uniauth.server.data.entity.RolePermissionKey;
import com.dianrong.common.uniauth.server.data.entity.Tag;
import com.dianrong.common.uniauth.server.data.entity.TagExample;
import com.dianrong.common.uniauth.server.data.entity.TagExample.Criteria;
import com.dianrong.common.uniauth.server.data.entity.TagType;
import com.dianrong.common.uniauth.server.data.entity.TagTypeExample;
import com.dianrong.common.uniauth.server.data.entity.User;
import com.dianrong.common.uniauth.server.data.entity.UserExample;
import com.dianrong.common.uniauth.server.data.entity.UserGrpExample;
import com.dianrong.common.uniauth.server.data.entity.UserGrpKey;
import com.dianrong.common.uniauth.server.data.entity.UserPwdLog;
import com.dianrong.common.uniauth.server.data.entity.UserRoleExample;
import com.dianrong.common.uniauth.server.data.entity.UserRoleKey;
import com.dianrong.common.uniauth.server.data.entity.UserTagExample;
import com.dianrong.common.uniauth.server.data.entity.UserTagKey;
import com.dianrong.common.uniauth.server.data.mapper.DomainMapper;
import com.dianrong.common.uniauth.server.data.mapper.GrpMapper;
import com.dianrong.common.uniauth.server.data.mapper.GrpPathMapper;
import com.dianrong.common.uniauth.server.data.mapper.PermissionMapper;
import com.dianrong.common.uniauth.server.data.mapper.RoleCodeMapper;
import com.dianrong.common.uniauth.server.data.mapper.RoleMapper;
import com.dianrong.common.uniauth.server.data.mapper.RolePermissionMapper;
import com.dianrong.common.uniauth.server.data.mapper.TagMapper;
import com.dianrong.common.uniauth.server.data.mapper.TagTypeMapper;
import com.dianrong.common.uniauth.server.data.mapper.UserGrpMapper;
import com.dianrong.common.uniauth.server.data.mapper.UserMapper;
import com.dianrong.common.uniauth.server.data.mapper.UserPwdLogMapper;
import com.dianrong.common.uniauth.server.data.mapper.UserRoleMapper;
import com.dianrong.common.uniauth.server.data.mapper.UserTagMapper;
import com.dianrong.common.uniauth.server.data.query.UserPwdLogQueryParam;
import com.dianrong.common.uniauth.server.datafilter.DataFilter;
import com.dianrong.common.uniauth.server.datafilter.FieldType;
import com.dianrong.common.uniauth.server.datafilter.FilterType;
import com.dianrong.common.uniauth.server.exp.AppException;
import com.dianrong.common.uniauth.server.mq.UniauthSender;
import com.dianrong.common.uniauth.server.support.CxfHeaderHolder;
import com.dianrong.common.uniauth.server.util.BeanConverter;
import com.dianrong.common.uniauth.server.util.CheckEmpty;
import com.dianrong.common.uniauth.server.util.ParamCheck;
import com.dianrong.common.uniauth.server.util.UniBundle;
import com.google.common.collect.Lists;

/**
 * Created by Arc on 14/1/16.
 */
@Service
public class UserService extends TenancyBasedService {

    private final static Logger logger = Logger.getLogger(UserService.class);

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private RoleCodeMapper roleCodeMapper;
    @Autowired
    private DomainMapper domainMapper;
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private CommonService commonService;
    @Autowired
    private GrpMapper grpMapper;
    @Autowired
    private UserGrpMapper userGrpMapper;
    @Autowired
    private GrpPathMapper grpPathMapper;
    @Autowired
    private UserTagMapper userTagMapper;
    @Autowired
    private TagMapper tagMapper;
    @Autowired
    private TagTypeMapper tagTypeMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    @Autowired
    private UniauthSender uniauthSender;

    @Autowired
    private UserExtendValService userExtendValService;

    @Autowired
    private UserPwdLogMapper userPwdLogMapper;

    /**
     * . 进行用户数据过滤的filter
     */
    @Resource(name = "userDataFilter")
    private DataFilter dataFilter;

    private static final ExecutorService executor = Executors.newFixedThreadPool(1);

    @Transactional
    public UserDto addNewUser(String name, String phone, String email) {
        this.checkPhoneAndEmail(phone, email, null);
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setTenancyId(tenancyService.getOneCanUsedTenancyId());
        Date now = new Date();
        user.setFailCount(AppConstants.ZERO_Byte);

        String randomPassword = AuthUtils.randomPassword();
        byte salt[] = AuthUtils.createSalt();
        user.setPassword(Base64.encode(AuthUtils.digest(randomPassword, salt)));
        user.setPasswordSalt(Base64.encode(salt));

        user.setLastUpdate(now);
        user.setCreateDate(now);
        user.setPhone(phone);
        user.setStatus(AppConstants.STATUS_ENABLED);
        userMapper.insert(user);
        UserDto userDto = BeanConverter.convert(user);

        // 用户添加成功后发送mq
        uniauthSender.sendUserAdd(userDto);
        userDto.setPassword(randomPassword);
        asynAddUserPwdLog(user);
        return userDto;
    }

    @Transactional
    public UserDto updateUser(UserActionEnum userActionEnum, Long id, String name, String phone, String email, String password, String orginPassword, Byte status) {
        if (userActionEnum == null || id == null) {
            throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("common.parameter.empty", "userActionEnum, userId"));
        }
        User user = userMapper.selectByPrimaryKey(id);
        if (user == null) {
            throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("common.entity.notfound", id, User.class.getSimpleName()));
        } else if (AppConstants.ONE_Byte.equals(user.getStatus()) && !UserActionEnum.STATUS_CHANGE.equals(userActionEnum)) {
            throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("common.entity.status.isone", id, User.class.getSimpleName()));
        }
        switch (userActionEnum) {
            case LOCK :
                user.setFailCount(AppConstants.MAX_AUTH_FAIL_COUNT);
                break;
            case UNLOCK :
                user.setFailCount(AppConstants.ZERO_Byte);
                break;
            case RESET_PASSWORD :
                checkUserPwd(id, password);
                byte salt[] = AuthUtils.createSalt();
                user.setPassword(Base64.encode(AuthUtils.digest(password, salt)));
                user.setPasswordSalt(Base64.encode(salt));
                user.setPasswordDate(new Date());
                // reset failed count
                user.setFailCount(AppConstants.ZERO_Byte);
                // log
                asynAddUserPwdLog(user);
                break;
            case STATUS_CHANGE :
                // 只处理启用的情况
                if (status != null && status == AppConstants.STATUS_ENABLED) {
                    this.checkPhoneAndEmail(user.getPhone(), user.getEmail(), user.getId());
                }
                user.setStatus(status);
                break;
            case UPDATE_INFO :
                this.checkPhoneAndEmail(phone, email, id);
                user.setName(name);
                user.setEmail(email);
                user.setPhone(phone);
                break;
            case RESET_PASSWORD_AND_CHECK :
                // 原始密码验证通过
                if (orginPassword == null) {
                    throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("common.parameter.origin.password.wrong"));
                }
                if (!UniPasswordEncoder.isPasswordValid(user.getPassword(), orginPassword, user.getPasswordSalt())) {
                    throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("common.parameter.wrong", "origin password"));
                }
                // 验证新密码
                checkUserPwd(id, password);
                byte salttemp[] = AuthUtils.createSalt();
                user.setPassword(Base64.encode(AuthUtils.digest(password, salttemp)));
                user.setPasswordSalt(Base64.encode(salttemp));
                user.setPasswordDate(new Date());
                user.setFailCount(AppConstants.ZERO_Byte);
                // log
                asynAddUserPwdLog(user);
                break;
        }
        user.setLastUpdate(new Date());
        userMapper.updateByPrimaryKey(user);
        return BeanConverter.convert(user).setPassword(password);
    }

    public List<RoleDto> getAllRolesToUser(Long userId, Integer domainId) {
        if (userId == null || domainId == null) {
            throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("common.parameter.empty", "userId, domainId"));
        }
        // 1. get all roles under the domain
        RoleExample roleExample = new RoleExample();
        roleExample.createCriteria().andDomainIdEqualTo(domainId).andStatusEqualTo(AppConstants.ZERO_Byte).andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
        List<Role> roles = roleMapper.selectByExample(roleExample);
        if (CollectionUtils.isEmpty(roles)) {
            return null;
        }
        // 2. get the checked roleIds for the user
        UserRoleExample userRoleExample = new UserRoleExample();
        userRoleExample.createCriteria().andUserIdEqualTo(userId);
        List<UserRoleKey> userRoleKeys = userRoleMapper.selectByExample(userRoleExample);
        Set<Integer> roleIds = null;
        if (!CollectionUtils.isEmpty(userRoleKeys)) {
            roleIds = new TreeSet<>();
            for (UserRoleKey userRoleKey : userRoleKeys) {
                roleIds.add(userRoleKey.getRoleId());
            }
        }

        List<RoleCode> roleCodes = roleCodeMapper.selectByExample(new RoleCodeExample());

        // build roleCode index.
        Map<Integer, String> roleCodeIdNamePairs = new TreeMap<>();
        for (RoleCode roleCode : roleCodes) {
            roleCodeIdNamePairs.put(roleCode.getId(), roleCode.getCode());
        }

        // 3. construct all roles under the domain & mark the role checked on
        // the user or not
        List<RoleDto> roleDtos = new ArrayList<>();
        for (Role role : roles) {
            RoleDto roleDto = new RoleDto().setId(role.getId()).setName(role.getName()).setRoleCode(roleCodeIdNamePairs.get(role.getRoleCodeId()));
            if (roleIds != null && roleIds.contains(role.getId())) {
                roleDto.setChecked(Boolean.TRUE);
            } else {
                roleDto.setChecked(Boolean.FALSE);
            }
            roleDtos.add(roleDto);
        }
        return roleDtos;
    }

    @Transactional
    public void saveRolesToUser(Long userId, List<Integer> roleIds) {
        if (userId == null || CollectionUtils.isEmpty(roleIds)) {
            throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("common.parameter.empty", "userId, roleIds"));
        }

        UserRoleExample userRoleExample = new UserRoleExample();
        userRoleExample.createCriteria().andUserIdEqualTo(userId);
        List<UserRoleKey> userRoleKeys = userRoleMapper.selectByExample(userRoleExample);
        Set<Integer> roleIdSet = new TreeSet<>();
        if (!CollectionUtils.isEmpty(userRoleKeys)) {
            for (UserRoleKey userRoleKey : userRoleKeys) {
                roleIdSet.add(userRoleKey.getRoleId());
            }
        }
        for (Integer roleId : roleIds) {
            if (!roleIdSet.contains(roleId)) {
                UserRoleKey userRoleKey = new UserRoleKey();
                userRoleKey.setRoleId(roleId);
                userRoleKey.setUserId(userId);
                userRoleMapper.insert(userRoleKey);
            }
        }
    }

    public PageDto<UserDto> searchUser(Long userId, Integer groupId, Boolean needDescendantGrpUser, Boolean needDisabledGrpUser, Integer roleId, List<Long> userIds, List<Long> excludeUserIds, String name, String phone, String email, Byte status, Integer tagId,
            Boolean needTag, Integer pageNumber, Integer pageSize) {
        if (pageNumber == null || pageSize == null) {
            throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("common.parameter.empty", "pageNumber, pageSize"));
        }
        UserExample userExample = new UserExample();
        userExample.setOrderByClause("create_date desc");
        userExample.setPageOffSet(pageNumber * pageSize);
        userExample.setPageSize(pageSize);
        UserExample.Criteria criteria = userExample.createCriteria();
        if (name != null) {
            criteria.andNameLike("%" + name + "%");
        }
        if (phone != null) {
            criteria.andPhoneLike("%" + phone + "%");
        }
        if (email != null) {
            criteria.andEmailLike("%" + email + "%");
        }
        if (status != null) {
            criteria.andStatusEqualTo(status);
        }
        if (userId != null) {
            criteria.andIdEqualTo(userId);
        }
        if (!CollectionUtils.isEmpty(userIds)) {
            criteria.andIdIn(userIds);
        }
        if (!CollectionUtils.isEmpty(excludeUserIds)) {
            criteria.andIdNotIn(excludeUserIds);
        }
        criteria.andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
        if (groupId != null) {
            UserGrpExample userGrpExample = new UserGrpExample();
            UserGrpExample.Criteria userGrpExampleCriteria = userGrpExample.createCriteria();
            if (needDescendantGrpUser != null && needDescendantGrpUser) {
                GrpPathExample grpPathExample = new GrpPathExample();
                grpPathExample.createCriteria().andAncestorEqualTo(groupId);
                List<GrpPath> grpPathes = grpPathMapper.selectByExample(grpPathExample);
                if (CollectionUtils.isEmpty(grpPathes)) {
                    return null;
                } else {
                    List<Integer> descendantIds = Lists.newArrayList();
                    for (GrpPath grpPath : grpPathes) {
                        descendantIds.add(grpPath.getDescendant());
                    }
                    if (needDisabledGrpUser != null && needDisabledGrpUser) {
                        // 查询所有子组 不管是否是禁用的
                        userGrpExampleCriteria.andGrpIdIn(descendantIds);
                    } else {
                        // 默认需要过滤掉禁用的组
                        GrpExample grpExample = new GrpExample();
                        grpExample.createCriteria().andIdIn(descendantIds).andStatusEqualTo(AppConstants.ZERO_Byte).andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
                        List<Grp> grps = grpMapper.selectByExample(grpExample);
                        if (CollectionUtils.isEmpty(grps)) {
                            return null;
                        } else {
                            List<Integer> enabledGrpIds = Lists.newArrayList();
                            for (Grp grp : grps) {
                                enabledGrpIds.add(grp.getId());
                            }
                            userGrpExampleCriteria.andGrpIdIn(enabledGrpIds);
                        }
                    }
                }
            } else {
                userGrpExampleCriteria.andGrpIdEqualTo(groupId);
            }
            userGrpExampleCriteria.andTypeEqualTo(AppConstants.ZERO_Byte);
            List<UserGrpKey> userGrpKeys = userGrpMapper.selectByExample(userGrpExample);
            if (CollectionUtils.isEmpty(userGrpKeys)) {
                return null;
            } else {
                List<Long> userGrpUserIds = new ArrayList<>();
                for (UserGrpKey userGrpKey : userGrpKeys) {
                    userGrpUserIds.add(userGrpKey.getUserId());
                }
                criteria.andIdIn(userGrpUserIds);
            }
        }
        if (roleId != null) {
            UserRoleExample userRoleExample = new UserRoleExample();
            UserRoleExample.Criteria userRoleExampleCriteria = userRoleExample.createCriteria();
            userRoleExampleCriteria.andRoleIdEqualTo(roleId);
            List<UserRoleKey> userRoleKeys = userRoleMapper.selectByExample(userRoleExample);
            if (CollectionUtils.isEmpty(userRoleKeys)) {
                return null;
            } else {
                List<Long> userRoleIds = new ArrayList<>();
                for (UserRoleKey userRoleKey : userRoleKeys) {
                    userRoleIds.add(userRoleKey.getUserId());
                }
                criteria.andIdIn(userRoleIds);
            }
        }
        if (tagId != null) {
            UserTagExample userTagExample = new UserTagExample();
            userTagExample.createCriteria().andTagIdEqualTo(tagId);
            List<UserTagKey> userTagKeys = userTagMapper.selectByExample(userTagExample);
            if (!CollectionUtils.isEmpty(userTagKeys)) {
                List<Long> userTagKeysUserIds = new ArrayList<>();
                for (UserTagKey userTagKey : userTagKeys) {
                    userTagKeysUserIds.add(userTagKey.getUserId());
                }
                criteria.andIdIn(userTagKeysUserIds);
            } else {
                return null;
            }
        }
        int count = userMapper.countByExample(userExample);
        ParamCheck.checkPageParams(pageNumber, pageSize, count);
        List<User> users = userMapper.selectByExample(userExample);
        if (!CollectionUtils.isEmpty(users)) {
            List<UserDto> userDtos = new ArrayList<>();
            Map<Long, UserDto> userIdUserDtoPair = new HashMap<>();
            for (User user : users) {
                UserDto userDto = BeanConverter.convert(user);
                userIdUserDtoPair.put(user.getId(), userDto);
                userDtos.add(userDto);
            }
            if (needTag != null && needTag) {
                // 1. query all tagIds and index them with userIds
                UserTagExample userTagExample = new UserTagExample();
                userTagExample.createCriteria().andUserIdIn(new ArrayList<Long>(userIdUserDtoPair.keySet()));
                List<UserTagKey> userTagKeys = userTagMapper.selectByExample(userTagExample);
                if (!CollectionUtils.isEmpty(userTagKeys)) {
                    Map<Integer, List<Long>> tagIdUserIdsPair = new HashMap<>();
                    for (UserTagKey userTagKey : userTagKeys) {
                        Long userId1 = userTagKey.getUserId();
                        Integer userTagId = userTagKey.getTagId();
                        List<Long> userIds1 = tagIdUserIdsPair.get(userTagId);
                        if (userIds1 == null) {
                            userIds1 = new ArrayList<>();
                            tagIdUserIdsPair.put(userTagId, userIds1);
                        }
                        userIds1.add(userId1);
                    }
                    // 2. query all tags, convert into dto and index them with
                    // tagIds
                    TagExample tagExample = new TagExample();
                    tagExample.createCriteria().andIdIn(new ArrayList<Integer>(tagIdUserIdsPair.keySet())).andStatusEqualTo(AppConstants.STATUS_ENABLED).andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
                    List<Tag> tags = tagMapper.selectByExample(tagExample);
                    if (!CollectionUtils.isEmpty(tags)) {
                        Map<Integer, TagDto> tagIdTagDtoPair = new HashMap<>();
                        List<Integer> tagTypeIds = new ArrayList<>();
                        for (Tag tag : tags) {
                            tagTypeIds.add(tag.getTagTypeId());
                            tagIdTagDtoPair.put(tag.getId(), BeanConverter.convert(tag));
                        }
                        // 3. query tagTypes info and index them with tagTypeId
                        TagTypeExample tagTypeExample = new TagTypeExample();
                        tagTypeExample.createCriteria().andIdIn(tagTypeIds).andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
                        List<TagType> tagTypes = tagTypeMapper.selectByExample(tagTypeExample);
                        Map<Integer, String> tagTypeIdTagCodePair = new HashMap<>();
                        for (TagType tagType : tagTypes) {
                            tagTypeIdTagCodePair.put(tagType.getId(), tagType.getCode());
                        }
                        Collection<TagDto> tagDtos = tagIdTagDtoPair.values();
                        for (TagDto tagDto : tagDtos) {
                            // 4. construct tagtype info into tagDto
                            String tagTypeCode = tagTypeIdTagCodePair.get(tagDto.getTagTypeId());
                            tagDto.setTagTypeCode(tagTypeCode);
                            // 5. construct tagDto into userDto
                            List<Long> userIds1 = tagIdUserIdsPair.get(tagDto.getId());
                            for (Long userId1 : userIds1) {
                                UserDto userDto = userIdUserDtoPair.get(userId1);
                                List<TagDto> tagDtoList = userDto.getTagDtos();
                                if (tagDtoList == null) {
                                    tagDtoList = new ArrayList<>();
                                    userDto.setTagDtos(tagDtoList);
                                }
                                tagDtoList.add(tagDto);
                            }
                        }
                    }
                }
            }
            return new PageDto<>(pageNumber, pageSize, count, userDtos);
        } else {
            return null;
        }
    }

    private void checkPhoneAndEmail(String phone, String email, Long userId) {
        if (email == null) {
            throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("common.parameter.empty", "email"));
        }
        if (!email.contains("@")) {
            throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("user.parameter.email.invalid", email));
        }
        // check duplicate email
        if (userId == null) {
            dataFilter.addFieldCheck(FilterType.FILTER_TYPE_EXSIT_DATA, FieldType.FIELD_TYPE_EMAIL, email);
        } else {
            dataFilter.updateFieldCheck(Integer.parseInt(userId.toString()), FieldType.FIELD_TYPE_EMAIL, email);
        }
        if (phone != null) {
            // check duplicate phone
            if (userId == null) {
                dataFilter.addFieldCheck(FilterType.FILTER_TYPE_EXSIT_DATA, FieldType.FIELD_TYPE_PHONE, phone);
            } else {
                dataFilter.updateFieldCheck(Integer.parseInt(userId.toString()), FieldType.FIELD_TYPE_PHONE, phone);
            }
        }
    }

    public UserDto login(LoginParam loginParam) {
        String account = loginParam.getAccount();
        String password = loginParam.getPassword();
        String ip = loginParam.getIp();
        CheckEmpty.checkEmpty(account, "账号");
        CheckEmpty.checkEmpty(password, "密码");
        CheckEmpty.checkEmpty(ip, "IP地址");

        User user = getUserByAccount(account.trim(), loginParam.getTenancyCode(), loginParam.getTenancyId(), true);

        if (AppConstants.ONE_Byte.equals(user.getStatus())) {
            throw new AppException(InfoName.LOGIN_ERROR_STATUS_1, UniBundle.getMsg("user.login.status.lock"));
        }
        if (user.getFailCount() >= AppConstants.MAX_AUTH_FAIL_COUNT) {
            throw new AppException(InfoName.LOGIN_ERROR_EXCEED_MAX_FAIL_COUNT, UniBundle.getMsg("user.login.account.lock"));
        }
        if (!UniPasswordEncoder.isPasswordValid(user.getPassword(), password, user.getPasswordSalt())) {
            updateLogin(user.getId(), ip, user.getFailCount() + 1, true);
            throw new AppException(InfoName.LOGIN_ERROR, UniBundle.getMsg("user.login.error"));
        }
        // successfully loged in
        updateLogin(user.getId(), ip, 0, false);

        Date passwordDate = user.getPasswordDate();
        if (passwordDate == null) {
            throw new AppException(InfoName.LOGIN_ERROR_NEW_USER, UniBundle.getMsg("user.login.newuser"));
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(user.getPasswordDate());
            calendar.add(Calendar.MONTH, AppConstants.MAX_PASSWORD_VALID_MONTH);
            Date currentDate = new Date();
            if (currentDate.after(calendar.getTime())) {
                throw new AppException(InfoName.LOGIN_ERROR_EXCEED_MAX_PASSWORD_VALID_MONTH, UniBundle.getMsg("user.login.password.usetoolong", String.valueOf(AppConstants.MAX_PASSWORD_VALID_MONTH)));
            }
        }
        return BeanConverter.convert(user);
    }

    public List<UserDto> searchUsersWithRoleCheck(Integer roleId) {
        CheckEmpty.checkEmpty(roleId, "roleId");
        UserExample userExample = new UserExample();
        userExample.createCriteria().andStatusEqualTo(AppConstants.STATUS_ENABLED).andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
        List<User> allUsers = userMapper.selectByExample(userExample);

        UserRoleExample userRoleExample = new UserRoleExample();
        userRoleExample.createCriteria().andRoleIdEqualTo(roleId);
        List<UserRoleKey> userRoleKeys = userRoleMapper.selectByExample(userRoleExample);
        List<UserDto> userDtos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userRoleKeys)) {
            Set<Long> userIdsLinkedToRole = new HashSet<>();
            for (UserRoleKey userRoleKey : userRoleKeys) {
                userIdsLinkedToRole.add(userRoleKey.getUserId());
            }
            for (User user : allUsers) {
                UserDto userDto = BeanConverter.convert(user);
                if (userIdsLinkedToRole.contains(user.getId())) {
                    userDto.setRoleChecked(Boolean.TRUE);
                }
                userDtos.add(userDto);
            }
        } else {
            for (User user : allUsers) {
                userDtos.add(BeanConverter.convert(user));
            }
        }
        return userDtos;
    }

    public List<UserDto> searchUserByRoleIds(List<Integer> roleIds) {
        CheckEmpty.checkEmpty(roleIds, "roleId");
        List<UserDto> userDtos = new ArrayList<>();
        if (roleIds.isEmpty()) {
            return userDtos;
        }
        UserRoleExample userRoleExample = new UserRoleExample();
        userRoleExample.createCriteria().andRoleIdIn(roleIds);
        List<UserRoleKey> userRoleKeys = userRoleMapper.selectByExample(userRoleExample);
        if (!CollectionUtils.isEmpty(userRoleKeys)) {
            List<Long> userIdsLinkedToRole = new ArrayList<>();
            for (UserRoleKey userRoleKey : userRoleKeys) {
                userIdsLinkedToRole.add(userRoleKey.getUserId());
            }
            UserExample userExample = new UserExample();
            userExample.createCriteria().andIdIn(userIdsLinkedToRole).andStatusEqualTo(AppConstants.STATUS_ENABLED).andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
            List<User> users = userMapper.selectByExample(userExample);
            if (users != null && !users.isEmpty()) {
                for (User user : users) {
                    userDtos.add(BeanConverter.convert(user));
                }
            }
        }
        return userDtos;
    }

    public List<UserDto> searchUsersWithTagCheck(Integer tagId) {
        CheckEmpty.checkEmpty(tagId, "tagId");
        UserExample userExample = new UserExample();
        userExample.createCriteria().andStatusEqualTo(AppConstants.STATUS_ENABLED).andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
        List<User> allUsers = userMapper.selectByExample(userExample);

        UserTagExample userTagExample = new UserTagExample();
        userTagExample.createCriteria().andTagIdEqualTo(tagId);

        List<UserTagKey> userTagKeys = userTagMapper.selectByExample(userTagExample);
        List<UserDto> userDtos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userTagKeys)) {
            Set<Long> userIdsLinkedToTag = new HashSet<>();
            for (UserTagKey userTagKey : userTagKeys) {
                userIdsLinkedToTag.add(userTagKey.getUserId());
            }
            for (User user : allUsers) {
                UserDto userDto = BeanConverter.convert(user);
                if (userIdsLinkedToTag.contains(user.getId())) {
                    userDto.setTagChecked(Boolean.TRUE);
                }
                userDtos.add(userDto);
            }
        } else {
            for (User user : allUsers) {
                userDtos.add(BeanConverter.convert(user));
            }
        }
        return userDtos;
    }

    public List<UserDto> searchUserByTagIds(List<Integer> tagIds) {
        CheckEmpty.checkEmpty(tagIds, "tagId");
        List<UserDto> userDtos = new ArrayList<>();
        if (tagIds.isEmpty()) {
            return userDtos;
        }
        UserTagExample userTagExample = new UserTagExample();
        userTagExample.createCriteria().andTagIdIn(tagIds);
        List<UserTagKey> userTagKeys = userTagMapper.selectByExample(userTagExample);
        if (!CollectionUtils.isEmpty(userTagKeys)) {
            List<Long> userIdsLinkedToTag = new ArrayList<>();
            for (UserTagKey usertagKey : userTagKeys) {
                userIdsLinkedToTag.add(usertagKey.getUserId());
            }
            UserExample userExample = new UserExample();
            userExample.createCriteria().andIdIn(userIdsLinkedToTag).andStatusEqualTo(AppConstants.STATUS_ENABLED).andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
            List<User> users = userMapper.selectByExample(userExample);
            if (users != null && !users.isEmpty()) {
                for (User user : users) {
                    userDtos.add(BeanConverter.convert(user));
                }
            }
        }
        return userDtos;
    }

    public UserDetailDto getUserDetailInfoByUid(Long paramUserId) {
        CheckEmpty.checkEmpty(paramUserId, "userId");
        User user = userMapper.selectByPrimaryKey(paramUserId);
        if (user == null) {
            return null;
        }
        // 手动设置tenancyId
        CxfHeaderHolder.TENANCYID.set(user.getTenancyId());
        UserDetailDto userDetailDto = getUserDetailDto(user);
        return userDetailDto;
    }

    public UserDetailDto getUserDetailInfo(LoginParam loginParam) {
        String account = loginParam.getAccount();
        CheckEmpty.checkEmpty(account, "账号");
        User user = getUserByAccount(account, loginParam.getTenancyCode(), loginParam.getTenancyId(), true);
        UserDetailDto userDetailDto = getUserDetailDto(user);
        return userDetailDto;
    }

    @SuppressWarnings("unchecked")
    private UserDetailDto getUserDetailDto(User user) {
        UserDetailDto userDetailDto = new UserDetailDto();
        UserDto userDto = BeanConverter.convert(user);
        setUserExtendVal(userDto);
        userDetailDto.setUserDto(userDto);

        Long userId = user.getId();

        Set<Integer> userAllRoleIds = new HashSet<>();
        UserRoleExample userRoleExample = new UserRoleExample();
        UserRoleExample.Criteria userRoleExCrieria = userRoleExample.createCriteria();
        userRoleExCrieria.andUserIdEqualTo(userId);
        // roleIds user direct connected.
        List<UserRoleKey> userRoleKeys = userRoleMapper.selectByExample(userRoleExample);
        if (!CollectionUtils.isEmpty(userRoleKeys)) {
            for (UserRoleKey userRoleKey : userRoleKeys) {
                userAllRoleIds.add(userRoleKey.getRoleId());
            }
        }
        // roleIds user extended from groups.
        List<Integer> roleIdsExtendedFromGrp = roleMapper.selectRoleIdsExtendedFromGrp(userId);
        if (!CollectionUtils.isEmpty(roleIdsExtendedFromGrp)) {
            userAllRoleIds.addAll(roleIdsExtendedFromGrp);
        }

        if (CollectionUtils.isEmpty(userAllRoleIds)) {
            return userDetailDto;
        }

        RoleExample roleExample = new RoleExample();
        RoleExample.Criteria roleCriteria = roleExample.createCriteria();
        roleCriteria.andIdIn(new ArrayList<>(userAllRoleIds)).andStatusEqualTo(AppConstants.STATUS_ENABLED).andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
        List<Role> roles = roleMapper.selectByExample(roleExample);

        if (CollectionUtils.isEmpty(roles)) {
            return userDetailDto;
        }

        List<Integer> domainIds = new ArrayList<>();
        Map<Integer, List<Role>> domainRoleMap = new HashMap<>();
        List<Integer> enabledAllRoleIds = new ArrayList<>();
        for (Role role : roles) {
            Integer domainId = role.getDomainId();
            domainIds.add(domainId);
            List<Role> domainRoles = domainRoleMap.get(domainId);
            if (CollectionUtils.isEmpty(domainRoles)) {
                domainRoles = new ArrayList<>();
                domainRoleMap.put(domainId, domainRoles);
            }
            domainRoles.add(role);
            enabledAllRoleIds.add(role.getId());
        }

        DomainExample domainExample = new DomainExample();
        DomainExample.Criteria criteria = domainExample.createCriteria();
        criteria.andIdIn(domainIds).andStatusEqualTo(AppConstants.STATUS_ENABLED).andTenancyIdEqualTo(tenancyService.getDefaultTenancy().getId());
        List<Domain> domainList = domainMapper.selectByExample(domainExample);
        List<DomainDto> domainDtoList = null;
        if (domainList == null || domainList.isEmpty()) {
            domainDtoList = Collections.EMPTY_LIST;
        } else {
            // add refactor
            Map<Integer, List<Permission>> roleIdPermissionsMap = getRolePermission(enabledAllRoleIds);
            // modify refactor
            domainDtoList = domainBean2DtoList(domainList, roleIdPermissionsMap, domainRoleMap);
        }
        userDetailDto.setDomainList(domainDtoList);
        return userDetailDto;
    }

    /**
     * 获取角色下面所有的权限
     * 
     * @param enableRoleIds
     *            可用的角色id集合
     * @return roleId与对应的权限集合映射;<br/>
     *         1.如果角色没有任何权限,那么角色的权限是空;<br/>
     *         2.如果没有任何角色,那么返回empty map
     */
    @SuppressWarnings("unchecked")
    private Map<Integer, List<Permission>> getRolePermission(List<Integer> enableRoleIds) {
        RolePermissionExample rolePermissionExample = new RolePermissionExample();
        RolePermissionExample.Criteria rolePermissionExampleCriteria = rolePermissionExample.createCriteria();
        rolePermissionExampleCriteria.andRoleIdIn(enableRoleIds);
        List<RolePermissionKey> rolePermissionKeys = rolePermissionMapper.selectByExample(rolePermissionExample);
        if (CollectionUtils.isEmpty(rolePermissionKeys))
            return Collections.EMPTY_MAP;// 2.没有角色
        // 权限id集合
        List<Integer> allPermissionIds = new ArrayList<>(rolePermissionKeys.size());
        // map permission roles
        Map<Integer, List<Integer>> permIdRoleIdsMap = new HashMap<>();
        for (RolePermissionKey rolePermissionKey : rolePermissionKeys) {
            Integer roleId = rolePermissionKey.getRoleId();
            Integer permissionId = rolePermissionKey.getPermissionId();
            allPermissionIds.add(permissionId);
            List<Integer> roleIds = permIdRoleIdsMap.get(permissionId);
            if (roleIds == null) {
                roleIds = new ArrayList<>();
                permIdRoleIdsMap.put(permissionId, roleIds);
            }
            roleIds.add(roleId);
        }
        Map<Integer, List<Permission>> roleIdPermissionsMap = new HashMap<>();
        if (CollectionUtils.isEmpty(allPermissionIds)) {// 1.空有角色没有权限,see
                                                        // jira->UNIAZ-181
            for (RolePermissionKey rolePermissionKey : rolePermissionKeys) {
                roleIdPermissionsMap.put(rolePermissionKey.getRoleId(), Collections.EMPTY_LIST);
            }
            return roleIdPermissionsMap;
        }
        PermissionExample permissionExample = new PermissionExample();
        PermissionExample.Criteria permissionExampleCriteria = permissionExample.createCriteria();
        permissionExampleCriteria.andIdIn(allPermissionIds).andStatusEqualTo(AppConstants.STATUS_ENABLED).andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
        List<Permission> permissions = permissionMapper.selectByExample(permissionExample);

        // map role permissions
        for (Permission permission : permissions) {
            List<Integer> roleIds = permIdRoleIdsMap.get(permission.getId());
            if (roleIds != null) {
                for (Integer roleId : roleIds) {// 每一个角色都包含这个权限
                    List<Permission> permissionList = roleIdPermissionsMap.get(roleId);
                    if (permissionList == null) {
                        permissionList = new ArrayList<>();
                        roleIdPermissionsMap.put(roleId, permissionList);
                    }
                    permissionList.add(permission);
                }
            }
        }
        return roleIdPermissionsMap;
    }
    /**
     * 将domain数据库实体对象转为dto对象
     * 
     * @param domainList
     *            domain的数据库实体对象
     * @param roleIdPermissionsMap
     *            角色id/角色的权限集合的映射关系
     * @param domainRoleMap
     *            domain id/domain的角色集合的映射关系
     * @return
     */
    private List<DomainDto> domainBean2DtoList(List<Domain> domainList, Map<Integer, List<Permission>> roleIdPermissionsMap, Map<Integer, List<Role>> domainRoleMap) {
        // move
        List<DomainDto> domainDtoList = new ArrayList<DomainDto>(domainList.size());
        Map<Integer, RoleCode> roleCodeMap = commonService.getRoleCodeMap();
        Map<Integer, PermType> permTypeMap = commonService.getPermTypeMap();
        for (Domain domain : domainList) {
            Integer domainId = domain.getId();

            List<Role> roleList = domainRoleMap.get(domainId);
            List<RoleDto> roleDtoList = new ArrayList<RoleDto>();

            DomainDto domainDto = BeanConverter.convert(domain);
            domainDto.setRoleList(roleDtoList);
            domainDtoList.add(domainDto);

            if (roleList != null) {
                for (Role role : roleList) {
                    RoleDto roleDto = BeanConverter.convert(role);
                    roleDto.setRoleCode(roleCodeMap.get(role.getRoleCodeId()).getCode());
                    // add refactor
                    buildRolePermissionDto(roleDto, permTypeMap, roleIdPermissionsMap, domainId, role.getId());
                    roleDtoList.add(roleDto);
                }
            }
        }
        return domainDtoList;
    }

    /**
     * 封装角色的权限数据,最终确定某个role有某个domain的某些permission
     * 
     * @param roleDto
     *            角色dto,已经封装了角色名称等基本信息
     * @param permTypeMap
     *            权限类型映射数据
     * @param roleIdPermissionsMap
     *            角色id/权限集合映射关系，确定一个角色有哪些权限
     * @param domainId
     *            domainId用来映射domain的权限数据
     * @param roleId
     *            角色id
     */
    private void buildRolePermissionDto(RoleDto roleDto, Map<Integer, PermType> permTypeMap, Map<Integer, List<Permission>> roleIdPermissionsMap, Integer domainId, Integer roleId) {
        List<Permission> permissionList = roleIdPermissionsMap.get(roleId);
        List<Permission> permList = new ArrayList<>();
        if (permissionList != null) {
            for (Permission permission : permissionList) {
                if (domainId.equals(permission.getDomainId())) {
                    permList.add(permission);
                }
            }
        }

        Map<String, Set<String>> permMap = new HashMap<String, Set<String>>();
        Map<String, Set<PermissionDto>> permDtoMap = new HashMap<>();
        if (permList != null) {
            for (Permission permission : permList) {
                Integer permTypeId = permission.getPermTypeId();
                String permType = permTypeMap.get(permTypeId).getType();
                String value = permission.getValue();
                PermissionDto permissionDto = BeanConverter.convert(permission);

                if (permMap.containsKey(permType)) {
                    permMap.get(permType).add(value);
                    permDtoMap.get(permType).add(permissionDto);
                } else {
                    Set<String> set = new HashSet<>();
                    set.add(value);
                    permMap.put(permType, set);
                    Set<PermissionDto> permissionDtos = new HashSet<>();
                    permissionDtos.add(permissionDto);
                    permDtoMap.put(permType, permissionDtos);
                }
            }
        }

        roleDto.setPermMap(permMap);
        roleDto.setPermDtoMap(permDtoMap);
    }

    public UserDto getSingleUser(UserParam userParam) {
        String email = userParam.getEmail();
        CheckEmpty.checkEmpty(email, "邮件");
        User user = getUserByAccount(email, userParam.getTenancyCode(), userParam.getTenancyId(), false);
        UserDto userDto = BeanConverter.convert(user);
        setUserExtendVal(userDto);
        return userDto;
    }

    @Transactional
    public void resetPassword(UserParam userParam) {
        String email = userParam.getEmail();
        String password = userParam.getPassword();
        CheckEmpty.checkEmpty(email, "邮件");
        CheckEmpty.checkEmpty(password, "密码");
        User user = getUserByAccount(email, userParam.getTenancyCode(), userParam.getTenancyId(), false);
        if (!AuthUtils.validatePasswordRule(password)) {
            throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("user.parameter.password.rule"));
        }
        byte salt[] = AuthUtils.createSalt();
        user.setPassword(Base64.encode(AuthUtils.digest(password, salt)));
        user.setPasswordSalt(Base64.encode(salt));
        user.setPasswordDate(new Date());
        user.setFailCount(AppConstants.ZERO_Byte);
        userMapper.updateByPrimaryKey(user);
    }

    @Transactional
    public void replaceRolesToUser(Long userId, List<Integer> roleIds, Integer domainId) {
        CheckEmpty.checkEmpty(userId, "userId");
        CheckEmpty.checkEmpty(domainId, "domainId");
        // step 1. get roleIds in the specific domain.
        RoleExample roleExample = new RoleExample();
        roleExample.createCriteria().andDomainIdEqualTo(domainId).andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
        List<Role> roles = roleMapper.selectByExample(roleExample);
        List<Integer> roleIdsInDomain = new ArrayList<>();
        if (!CollectionUtils.isEmpty(roles)) {
            for (Role role : roles) {
                roleIdsInDomain.add(role.getId());
            }
        }
        // Not null, otherwise it is an invalid call.
        CheckEmpty.checkEmpty(roleIdsInDomain, "roleIdsInDomain");
        UserRoleExample userRoleExample = new UserRoleExample();
        userRoleExample.createCriteria().andUserIdEqualTo(userId).andRoleIdIn(roleIdsInDomain);
        if (CollectionUtils.isEmpty(roleIds)) {
            userRoleMapper.deleteByExample(userRoleExample);
            return;
        }

        // if the input roleIds is not under the domain, then it is an invalid
        // call
        if (!roleIdsInDomain.containsAll(roleIds)) {
            throw new AppException(InfoName.BAD_REQUEST, UniBundle.getMsg("common.parameter.ids.invalid", roleIds));
        }

        List<UserRoleKey> userRoleKeys = userRoleMapper.selectByExample(userRoleExample);
        if (!CollectionUtils.isEmpty(userRoleKeys)) {
            ArrayList<Integer> dbRoleIds = new ArrayList<>();
            for (UserRoleKey userRoleKey : userRoleKeys) {
                dbRoleIds.add(userRoleKey.getRoleId());
            }
            @SuppressWarnings("unchecked")
            ArrayList<Integer> intersections = ((ArrayList<Integer>) dbRoleIds.clone());
            intersections.retainAll(roleIds);
            List<Integer> roleIdsNeedAddToDB = new ArrayList<>();
            List<Integer> roleIdsNeedDeleteFromDB = new ArrayList<>();
            for (Integer roleId : roleIds) {
                if (!intersections.contains(roleId)) {
                    roleIdsNeedAddToDB.add(roleId);
                }
            }
            for (Integer dbRoleId : dbRoleIds) {
                if (!intersections.contains(dbRoleId)) {
                    roleIdsNeedDeleteFromDB.add(dbRoleId);
                }
            }

            if (!CollectionUtils.isEmpty(roleIdsNeedAddToDB)) {
                for (Integer roleIdNeedAddToDB : roleIdsNeedAddToDB) {
                    UserRoleKey userRoleKey = new UserRoleKey();
                    userRoleKey.setRoleId(roleIdNeedAddToDB);
                    userRoleKey.setUserId(userId);
                    userRoleMapper.insert(userRoleKey);
                }
            }
            if (!CollectionUtils.isEmpty(roleIdsNeedDeleteFromDB)) {
                UserRoleExample userRoleDeleteExample = new UserRoleExample();
                userRoleDeleteExample.createCriteria().andUserIdEqualTo(userId).andRoleIdIn(roleIdsNeedDeleteFromDB);
                userRoleMapper.deleteByExample(userRoleDeleteExample);
            }
        } else {
            for (Integer roleId : roleIds) {
                UserRoleKey userRoleKey = new UserRoleKey();
                userRoleKey.setRoleId(roleId);
                userRoleKey.setUserId(userId);
                userRoleMapper.insert(userRoleKey);
            }
        }
    }

    private int updateLogin(Long userId, String ip, int failCount, boolean sync) {
        final User user = new User();
        user.setId(userId);
        user.setLastLoginTime(new Date());
        user.setLastLoginIp(ip);
        user.setFailCount((byte) failCount);
        if (sync) {
            return userMapper.updateByPrimaryKeySelective(user);
        }

        executor.submit(new Runnable() {
            @Override
            public void run() {
                userMapper.updateByPrimaryKeySelective(user);
            }
        });
        return 1;
    }

    private User getUserByAccount(String account, String tenancyCode, Integer tenancyId, boolean withPhoneChecked) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("email", account);

        if (withPhoneChecked) {
            map.put("phone", account);
        }
        if (tenancyCode == null && tenancyId == null) {
            map.put("tenancyId", tenancyService.getOneCanUsedTenancyId().toString());
        } else {
            if (tenancyId == null) {
                CheckEmpty.checkEmpty(tenancyCode, "租户code");
                map.put("tenancyCode", tenancyCode);
            } else {
                map.put("tenancyId", tenancyId.toString());
            }
        }
        List<User> userList = userMapper.selectByEmailOrPhone(map);
        if (userList == null || userList.isEmpty()) {
            throw new AppException(InfoName.LOGIN_ERROR_USER_NOT_FOUND, UniBundle.getMsg("user.login.notfound", account));
        }
        if (userList.size() > 1) {
            throw new AppException(InfoName.LOGIN_ERROR_MULTI_USER_FOUND, UniBundle.getMsg("user.login.multiuser.found"));
        }

        User user = userList.get(0);

        // 手动设置tenancyId -- important
        CxfHeaderHolder.TENANCYID.set(user.getTenancyId());
        return user;
    }

    private void setUserExtendVal(UserDto userDto) {
        List<UserExtendValDto> userExtendValDtos = userExtendValService.searchByUserId(userDto.getId(), AppConstants.STATUS_ENABLED);
        userDto.setUserExtendValDtos(userExtendValDtos);
    }

    /**
     * . 根据email或phone获取用户信息
     * 
     * @param loginParam
     *            email或phone
     * @return 信息model
     */
    public UserDto getUserByEmailOrPhone(LoginParam loginParam) {
        CheckEmpty.checkEmpty(loginParam.getAccount(), "账号");
        User user = getUserByAccount(loginParam.getAccount(), loginParam.getTenancyCode(), loginParam.getTenancyId(), true);
        UserDto userDto = BeanConverter.convert(user);
        setUserExtendVal(userDto);
        return userDto;
    }

    /**
     * . 获取所有的tags，并且根据用户id打上对应的checked标签
     * 
     * @param userId
     *            用户id
     * @param domainId
     *            域名id
     * @return List<TagDto>
     */
    public List<TagDto> searchTagsWithUserChecked(Long userId, Integer domainId) {
        CheckEmpty.checkEmpty(userId, "userId");
        CheckEmpty.checkEmpty(domainId, "domainId");

        // 获取tagType信息
        TagTypeExample tagTypeExample = new TagTypeExample();
        // 添加查询条件
        tagTypeExample.createCriteria().andDomainIdEqualTo(domainId).andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
        List<TagType> tagTypes = tagTypeMapper.selectByExample(tagTypeExample);
        if (tagTypes == null || tagTypes.isEmpty()) {
            return new ArrayList<TagDto>();
        }
        Map<Integer, TagType> tagTypeIdMap = new HashMap<Integer, TagType>();
        if (!CollectionUtils.isEmpty(tagTypes)) {
            for (TagType tagType : tagTypes) {
                tagTypeIdMap.put(tagType.getId(), tagType);
            }
        }

        // 查询用户和tag的关联关系信息
        UserTagExample userTagExample = new UserTagExample();
        userTagExample.createCriteria().andUserIdEqualTo(userId);
        List<UserTagKey> userTagKeys = userTagMapper.selectByExample(userTagExample);
        List<TagDto> tagDtos = new ArrayList<TagDto>();
        Set<Integer> tagIdLinkedToUser = new HashSet<Integer>();
        if (!CollectionUtils.isEmpty(userTagKeys)) {
            for (UserTagKey userTagKey : userTagKeys) {
                tagIdLinkedToUser.add(userTagKey.getTagId());
            }
        }

        // 查询tag信息
        TagExample tagConditon = new TagExample();
        Criteria andStatusEqualTo = tagConditon.createCriteria();
        andStatusEqualTo.andStatusEqualTo(AppConstants.STATUS_ENABLED);

        // 加入domainId的限制
        andStatusEqualTo.andTagTypeIdIn(new ArrayList<Integer>(tagTypeIdMap.keySet())).andTenancyIdEqualTo(tenancyService.getOneCanUsedTenancyId());
        List<Tag> allTags = tagMapper.selectByExample(tagConditon);

        // 优化
        if (allTags == null || allTags.isEmpty()) {
            return new ArrayList<TagDto>();
        }

        for (Tag tag : allTags) {
            TagDto tagDto = BeanConverter.convert(tag);
            if (tagIdLinkedToUser.contains(tagDto.getId())) {
                tagDto.setTagUserChecked(Boolean.TRUE);
            }

            if (tagTypeIdMap.get(tagDto.getTagTypeId()) != null) {
                tagDto.setTagTypeCode(tagTypeIdMap.get(tagDto.getTagTypeId()).getCode());
            } else {
                tagDto.setTagTypeCode("UNKNOW");
            }
            tagDtos.add(tagDto);
        }
        return tagDtos;
    }

    @Transactional
    public void replaceTagsToUser(Long userId, List<Integer> tagIds) {
        CheckEmpty.checkEmpty(userId, "userId");
        // step 1. delete all relationship
        UserTagExample delCondtion = new UserTagExample();
        delCondtion.createCriteria().andUserIdEqualTo(userId);
        userTagMapper.deleteByExample(delCondtion);

        // step2 .batch insert relationship
        if (tagIds == null) {
            throw new AppException(InfoName.BAD_REQUEST, UniBundle.getMsg("common.parameter.empty", "tagIds"));
        }

        if (tagIds.isEmpty()) {
            return;
        }

        List<UserTagKey> infoes = new ArrayList<UserTagKey>();
        for (Integer tagId : tagIds) {
            infoes.add(new UserTagKey().setUserId(userId).setTagId(tagId));
        }
        userTagMapper.bacthInsert(infoes);
    }

    /**
     * . 检验密码是否符合要求
     * 
     * @param userId
     *            userId
     * @param password
     *            the new password
     */
    private void checkUserPwd(Long userId, String password) {
        CheckEmpty.checkEmpty(userId, "userId");
        // check
        if (password == null) {
            throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("common.parameter.empty", "password"));
        }
        if (!AuthUtils.validatePasswordRule(password)) {
            throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("user.parameter.password.rule"));
        }
        UserPwdLogQueryParam condition = new UserPwdLogQueryParam();
        condition.setUserId(userId);
        Calendar time = Calendar.getInstance();
        time.add(Calendar.MONTH, -AppConstants.DUPLICATE_PWD_VALID_MONTH);
        condition.setCreateDateBegin(time.getTime());
        List<UserPwdLog> logs = userPwdLogMapper.queryUserPwdLogs(condition);
        if (logs == null || logs.isEmpty()) {
            return;
        }
        // check duplicate password
        for (UserPwdLog log : logs) {
            if (UniPasswordEncoder.isPasswordValid(log.getPassword(), password, log.getPasswordSalt())) {
                throw new AppException(InfoName.VALIDATE_FAIL, UniBundle.getMsg("user.parameter.password.duplicate", AppConstants.DUPLICATE_PWD_VALID_MONTH));
            }
        }
    }

    /**
     * . 异步记录用户的密码设置记录
     * 
     * @param user
     *            info
     */
    private void asynAddUserPwdLog(final User user) {
        Assert.notNull(user);
        // 异步添加UserPwdLog
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    UserPwdLog log = new UserPwdLog();
                    log.setUserId(user.getId());
                    log.setPassword(user.getPassword());
                    log.setPasswordSalt(user.getPasswordSalt());
                    log.setCreateDate(new Date());
                    log.setTenancyId(user.getTenancyId());
                    userPwdLogMapper.insert(log);
                } catch (Exception ex) {
                    logger.error("failed to log add new user pwd log", ex);
                }
            }
        });
    }
}
