/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.db.dao.user;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.openmeetings.db.dao.IGroupAdminDataProviderDao;
import org.apache.openmeetings.db.entity.user.Group;
import org.apache.openmeetings.util.DaoHelper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class GroupDao implements IGroupAdminDataProviderDao<Group> {
	private static final String[] searchFields = {"name"};
	@PersistenceContext
	private EntityManager em;

	@Override
	public Group get(long id) {
		return get(Long.valueOf(id));
	}

	@Override
	public Group get(Long id) {
		List<Group> list = em.createNamedQuery("getGroupById", Group.class)
				.setParameter("id", id).getResultList();
		return list.size() == 1 ? list.get(0) : null;
	}

	public Group get(String name) {
		List<Group> groups = em.createNamedQuery("getGroupByName", Group.class).setParameter("name", name).getResultList();
		return groups == null || groups.isEmpty() ? null : groups.get(0);
	}

	@Override
	public List<Group> get(int start, int count) {
		TypedQuery<Group> q = em.createNamedQuery("getNondeletedGroups", Group.class);
		q.setFirstResult(start);
		q.setMaxResults(count);
		return q.getResultList();
	}

	@Override
	public List<Group> get(String search, int start, int count, String sort) {
		TypedQuery<Group> q = em.createQuery(DaoHelper.getSearchQuery("Group", "g", search, true, false, sort, searchFields), Group.class);
		q.setFirstResult(start);
		q.setMaxResults(count);
		return q.getResultList();
	}

	@Override
	public List<Group> adminGet(String search, Long adminId, int start, int count, String order) {
		TypedQuery<Group> q = em.createQuery(DaoHelper.getSearchQuery("GroupUser gu, IN(gu.group)", "g", null, search, true, true, false
				, "gu.user.id = :adminId AND gu.moderator = true", order, searchFields), Group.class);
		q.setParameter("adminId", adminId);
		q.setFirstResult(start);
		q.setMaxResults(count);
		return q.getResultList();
	}

	@Override
	public long count() {
		TypedQuery<Long> q = em.createNamedQuery("countGroups", Long.class);
		return q.getSingleResult();
	}

	@Override
	public long count(String search) {
		TypedQuery<Long> q = em.createQuery(DaoHelper.getSearchQuery("Group", "o", search, true, true, null, searchFields), Long.class);
		return q.getSingleResult();
	}

	@Override
	public long adminCount(String search, Long adminId) {
		TypedQuery<Long> q = em.createQuery(DaoHelper.getSearchQuery("GroupUser gu, IN(gu.group)", "g", null, search, true, true, true
				, "gu.user.id = :adminId AND gu.moderator = true", null, searchFields), Long.class);
		q.setParameter("adminId", adminId);
		return q.getSingleResult();
	}

	public List<Group> get(Collection<Long> ids) {
		return em.createNamedQuery("getGroupsByIds", Group.class).setParameter("ids", ids).getResultList();
	}

	public List<Group> getLimited() {
		return em.createNamedQuery("getLimitedGroups", Group.class).getResultList();
	}

	@Override
	public Group update(Group entity, Long userId) {
		if (entity.getId() == null) {
			if (userId != null) {
				entity.setInsertedby(userId);
			}
			entity.setInserted(new Date());
			em.persist(entity);
		} else {
			if (userId != null) {
				entity.setUpdatedby(userId);
			}
			entity.setUpdated(new Date());
			em.merge(entity);
		}
		return entity;
	}

	@Override
	public void delete(Group g, Long userId) {
		em.createNamedQuery("deleteGroupUsersByGroup").setParameter("id", g.getId()).executeUpdate();

		g.setDeleted(true);
		if (userId != null) {
			g.setUpdatedby(userId);
		}
		em.merge(g);
	}
}
